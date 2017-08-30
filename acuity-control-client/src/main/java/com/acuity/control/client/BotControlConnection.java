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
import com.acuity.db.domain.vertex.impl.message_package.data.RemoteScript;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;
import com.google.common.eventbus.Subscribe;

import java.security.Permission;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class BotControlConnection {

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
        wsClient.send(new MessagePackage(MessagePackage.Type.LOGIN, null).setBody(
                new LoginData(acuityEmail, new String(acuityPassword), 1, botTypeID)
        ));
    }

    public Optional<String> decryptString(EncryptedString string){
        Optional.ofNullable(System.getSecurityManager()).ifPresent(securityManager -> securityManager.checkPermission(DECRYPT_STRING_PERMISSION));
        MessageResponse response = send(new MessagePackage(MessagePackage.Type.DECRYPT_STING, MessagePackage.SERVER)
                .setBody(0, string)
                .setBody(1, new String(acuityPassword))
        );
        return response.waitForResponse(15, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(String.class));
    }

    public MessageResponse sendWithCreds(MessagePackage messagePackage){
        messagePackage.setBody(0, acuityEmail);
        messagePackage.setBody(1, acuityPassword);
        return send(messagePackage);
    }

    public MessageResponse send(MessagePackage messagePackage){
        MessageResponse response = new MessageResponse();
        messagePackage.setResponseKey(UUID.randomUUID().toString());
        wsClient.getResponseTracker().getCache().put(messagePackage.getResponseKey(), response);
        wsClient.send(messagePackage);
        return response;
    }

    @Subscribe
    public void onMessage(MessagePackage messagePackage){
        if (messagePackage.getMessageType() == MessagePackage.Type.GOOD_LOGIN){
            wsClient.send(new MessagePackage(MessagePackage.Type.MACHINE_INFO, MessagePackage.SERVER).setBody(MachineUtil.buildMachineState()));
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.BAD_LOGIN){
            handleLogin();
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.CONFIG_UPDATE){
            BotClientConfig config = messagePackage.getBodyAs(BotClientConfig.class);
            botControl.getScriptManager().onBotClientConfigUpdate(config);
            botControl.getBreakManager().onBotClientConfigUpdate(config);
            botControl.getProxyManager().onBotClientConfigUpdate(config);
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.ACCOUNT_ASSIGNMENT_CHANGE){
            RSAccount account = messagePackage.getBodyAs(RSAccount.class);
            botControl.getRsAccountManager().onRSAccountAssignmentUpdate(account);
        }
        else if (messagePackage.getMessageType() == MessagePackage.Type.REQUEST_REMOTE_SCRIPT_QUEUE){
            Pair<ScriptExecutionConfig, Object> scriptInstance = botControl.getScriptManager().getScriptInstance();
            if (scriptInstance != null && scriptInstance.getValue() instanceof RemoteScriptStartCheck){
                if (!((RemoteScriptStartCheck) scriptInstance.getValue()).isAcceptingScriptStarts()){
                    botControl.respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey()).setBody(false));
                    return;
                }
            }

            RemoteScript.StartRequest scriptStartRequest = messagePackage.getBodyAs(RemoteScript.StartRequest.class);
            ScriptExecutionConfig executionConfig = scriptStartRequest.getExecutionConfig();
            RSAccount rsAccount = null;
            if (scriptStartRequest.isConditionalOnAccountAssignment()){
                rsAccount = botControl.getRsAccountManager().requestAccountFromTag(executionConfig.getScriptRunConfig().getPullAccountsFromTagID(), false);
            }

            RemoteScript.StartResponse result = new RemoteScript.StartResponse();
            result.setAccount(rsAccount);
            if (rsAccount != null || !scriptStartRequest.isConditionalOnAccountAssignment()){
                result.setScriptStarted(botControl.getScriptManager().queueStart(executionConfig));
                if (!result.isScriptStarted()) botControl.requestAccountAssignment(null, true);
            }

            botControl.respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey()).setBody(result));
        }
        else {
            botControl.getEventBus().post(messagePackage);
        }
    }

    public boolean isConnected() {
        return wsClient != null && wsClient.isConnected();
    }
}
