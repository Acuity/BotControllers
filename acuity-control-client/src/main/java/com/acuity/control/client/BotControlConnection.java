package com.acuity.control.client;

import com.acuity.common.security.PasswordStore;
import com.acuity.common.ui.LoginFrame;
import com.acuity.common.util.Pair;
import com.acuity.control.client.machine.MachineUtil;
import com.acuity.control.client.scripts.RemoteScriptStartCheck;
import com.acuity.control.client.websockets.WClientEvent;
import com.acuity.control.client.websockets.response.MessageResponse;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.common.EncryptedString;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.message_package.data.LoginData;
import com.acuity.db.domain.vertex.impl.message_package.data.RemoteScriptTask;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.google.common.eventbus.Subscribe;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Permission;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class BotControlConnection {

    private static final Logger logger = LoggerFactory.getLogger(BotControlConnection.class);
    private static final Permission DECRYPT_STRING_PERMISSION = new RuntimePermission("decryptString");

    private BotControl botControl;
    private String host;
    private String acuityEmail;
    private char[] acuityPassword;
    private int botTypeID;

    private LoginFrame loginFrame;

    private AcuityWSClient wsClient = new AcuityWSClient();

    public BotControlConnection(BotControl botControl, String host, ClientType clientType) {
        this.botControl = botControl;
        this.host = host;
        this.botTypeID = clientType.getID();
        handleLogin();
    }

    private void handleLogin(){
        try {
            String acuityEmail = PasswordStore.getAcuityEmail();
            String acuityPassword = PasswordStore.getAcuityPassword();
            if (acuityEmail != null && acuityPassword != null){
                start(acuityEmail, acuityPassword);
                return;
            }
        }
        catch (Exception ignored){
        }

        if (loginFrame != null) loginFrame.dispose();
        loginFrame = new LoginFrame() {
            @Override
            public void onLogin(String email, String password) {
                try {
                    start(email, password);
                    loginFrame.dispose();
                    loginFrame = null;
                } catch (Exception ignored) {
                }
            }
        };
        loginFrame.setVisible(true);
    }

    public void start(String email, String password) throws Exception {
        this.acuityEmail = email;
        this.acuityPassword = password.toCharArray();
        wsClient.getEventBus().register(this);
        wsClient.start("ws://" + host + ":2052");
    }

    public void stop(){
        try {
            wsClient.getEventBus().unregister(this);
        }catch (IllegalArgumentException ignored){
        }
        wsClient.stop();
    }

    @Subscribe
    public void onConnect(WClientEvent.Opened opened){
        wsClient.send(new MessagePackage(MessagePackage.Type.LOGIN, null)
                .setBody(0, new LoginData(acuityEmail, new String(acuityPassword), 1, botTypeID))
                .setBody(1, botControl.getBotClientConfig())
        );
    }

    public Optional<String> decryptString(EncryptedString string){
        Optional.ofNullable(System.getSecurityManager()).ifPresent(securityManager -> securityManager.checkPermission(DECRYPT_STRING_PERMISSION));
        MessageResponse response = send(new MessagePackage(MessagePackage.Type.DECRYPT_STING, MessagePackage.SERVER)
                .setBody(0, string)
                .setBody(1, new String(acuityPassword))
        );
        return response.waitForResponse(15, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(String.class));
    }

    public MessageResponse sendWithCredentials(MessagePackage messagePackage){
        messagePackage.setBody(0, acuityEmail);
        messagePackage.setBody(1, acuityPassword);
        return send(messagePackage);
    }

    public MessageResponse send(MessagePackage messagePackage){
        messagePackage.setResponseKey(UUID.randomUUID().toString());
        MessageResponse response = new MessageResponse(messagePackage.getResponseKey());
        wsClient.getResponseTracker().getCache().put(messagePackage.getResponseKey(), response);
        wsClient.send(messagePackage);
        logger.debug("Sent - {}.", messagePackage);
        return response;
    }

    @Subscribe
    public void onMessage(MessagePackage messagePackage){
        if (messagePackage.getMessageType() == MessagePackage.Type.GOOD_LOGIN){
            RSAccount rsAccount = botControl.getRsAccountManager().getRsAccount();
            if (rsAccount != null){
                if (!botControl.requestAccountAssignment(rsAccount, false)){
                    botControl.getRsAccountManager().clearRSAccount();
                }
            }

            wsClient.send(new MessagePackage(MessagePackage.Type.MACHINE_INFO, MessagePackage.SERVER).setBody(MachineUtil.buildMachineState()));
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.BAD_LOGIN){
            handleLogin();
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.CONFIG_UPDATE){
            BotClientConfig config = messagePackage.getBodyAs(BotClientConfig.class);
            botControl.onConfigUpdate(config);
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.ACCOUNT_ASSIGNMENT_CHANGE){
            RSAccount account = messagePackage.getBodyAs(RSAccount.class);
            botControl.getRsAccountManager().onRSAccountAssignmentUpdate(account);
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.REQUEST_REMOTE_TASK_START){
            logger.debug("onMessage - REQUEST_REMOTE_TASK_START");

            Pair<String, Object> scriptInstance = botControl.getScriptManager().getScriptInstance().orElse(null);
            if (scriptInstance != null && scriptInstance.getValue() instanceof RemoteScriptStartCheck){
                if (!((RemoteScriptStartCheck) scriptInstance.getValue()).isAcceptingScriptStarts()){
                    logger.debug("Remote Task Request - Current script not accepting new tasks.");
                    botControl.respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey())
                            .setBody(new RemoteScriptTask.StartResponse()));
                    return;
                }
            }

            RemoteScriptTask.StartRequest scriptStartRequest = messagePackage.getBodyAs(RemoteScriptTask.StartRequest.class);
            ScriptNode executionConfig = scriptStartRequest.getExecutionConfig();
            RSAccount rsAccount = null;

            String accountAssignmentTag = (String) executionConfig.getSettings().get("accountAssignmentTag");
            boolean registrationEnabled = (boolean) executionConfig.getSettings().get("registrationEnabled");
            if (scriptStartRequest.isConditionalOnAccountAssignment() && accountAssignmentTag != null){

                logger.debug("Remote Task Request - Conditional on account assignment, requesting account.");
                rsAccount = botControl.getRsAccountManager().requestAccountFromTag(accountAssignmentTag, true,false, registrationEnabled);
                logger.debug("Remote Task Request - Account assignment result. {}", rsAccount);
            }

            RemoteScriptTask.StartResponse result = new RemoteScriptTask.StartResponse();
            result.setAccount(rsAccount);
            if (rsAccount != null || !scriptStartRequest.isConditionalOnAccountAssignment()){
                logger.debug("Remote Task Request - Adding task to queue.");

                BotClientConfig botClientConfig = botControl.getBotClientConfig();
                botClientConfig.getTaskNodeList().add(0, executionConfig);
                if (!botControl.updateClientConfig(botClientConfig)) {
                    logger.debug("Remote Task Request - Failed to add task to queue, clearing account.");
                    botControl.getRsAccountManager().clearRSAccount();
                }
            }

            logger.debug("Remote Task Request - Sending result to requester. {}, {}", result, messagePackage.getSourceKey());
            botControl.respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey()).setBody(result));
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.REQUEST_SCREEN_CAP){
            sendScreenCapture(messagePackage.getBodyAs(Integer.class));
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.SEND_IN_GAME_MESSAGE){
            botControl.sendInGameMessage(messagePackage.getBodyAs(String.class));
        }
        else {
            botControl.getEventBus().post(messagePackage);
        }
    }

    public void sendScreenCapture(Integer scale) {
        BufferedImage screenCapture = botControl.getScreenCapture();

        if(scale != 0){
            try {
                screenCapture = Thumbnails.of(screenCapture)
                        .size(screenCapture.getWidth() / (2 * scale), screenCapture.getHeight() / (2 * scale))
                        .outputFormat("jpg")
                        .asBufferedImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (screenCapture != null){
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                ImageIO.write(screenCapture, "jpg", baos );
                baos.flush();
                byte[] imageBytes = baos.toByteArray();
                send(new MessagePackage(MessagePackage.Type.REQUEST_SCREEN_CAP, MessagePackage.SERVER).setBody(imageBytes));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return wsClient != null && wsClient.isConnected();
    }
}
