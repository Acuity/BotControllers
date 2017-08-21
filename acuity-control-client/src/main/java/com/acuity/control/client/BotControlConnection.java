package com.acuity.control.client;

import com.acuity.common.security.PasswordStore;
import com.acuity.common.ui.LoginFrame;
import com.acuity.control.client.websockets.WClientEvent;
import com.acuity.control.client.websockets.response.MessageResponse;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.common.EncryptedString;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.message_package.data.LoginData;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.google.common.eventbus.Subscribe;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class BotControlConnection {

    private BotControl botControl;
    private String host;
    private String acuityEmail;
    private String acuityPassword;
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
        this.acuityPassword = password;
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
                new LoginData(acuityEmail, acuityPassword, 1, botTypeID)
        ));
    }

    public Optional<String> decryptString(EncryptedString string){
        MessageResponse response = send(new MessagePackage(MessagePackage.Type.DECRYPT_STING, MessagePackage.SERVER)
                .setBody(0, string)
                .setBody(1, acuityPassword)
        );
        return response.waitForResponse(15, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(String.class));
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
            //sendMachineInfo();
            //onGoodLogin();
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
        else {
            botControl.getEventBus().post(messagePackage);
        }
    }

    public boolean isConnected() {
        return wsClient != null && wsClient.isConnected();
    }
}