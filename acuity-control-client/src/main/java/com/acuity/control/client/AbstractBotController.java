package com.acuity.control.client;

import com.acuity.common.security.PasswordStore;
import com.acuity.common.ui.LoginFrame;
import com.acuity.control.client.machine.MachineUtil;
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
 * Created by Zachary Herridge on 8/9/2017.
 */
public abstract class AbstractBotController {

    private String host;
    private String acuityEmail;
    private String acuityPassword;
    private int botTypeID;
    private LoginFrame loginFrame;

    public AbstractBotController(String host, int botTypeID) {
        this.host = host;
        this.botTypeID = botTypeID;
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
            //@Override
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
        AcuityWSClient.getInstance().getEventBus().register(this);
        AcuityWSClient.getInstance().start("ws://" + host + ":2052");
    }

    public void stop(){
        try {
            AcuityWSClient.getInstance().getEventBus().unregister(this);
        }catch (IllegalArgumentException ignored){
        }
        AcuityWSClient.getInstance().stop();
    }

    @Subscribe
    public void onConnect(WClientEvent.Opened opened){
        AcuityWSClient.getInstance().send(new MessagePackage(MessagePackage.Type.LOGIN, null).setBody(
                new LoginData(acuityEmail, acuityPassword, 1, botTypeID)
        ));
    }

    @Subscribe
    public void onMessage(MessagePackage messagePackage){
        if (messagePackage.getMessageType() == MessagePackage.Type.GOOD_LOGIN){
            sendMachineInfo();
            onGoodLogin();
        }

        if (messagePackage.getMessageType() == MessagePackage.Type.BAD_LOGIN){
            handleLogin();
            onBadLogin();
        }

        if (messagePackage.getMessageType() == MessagePackage.Type.CONFIG_UPDATE){
            BotClientConfig config = messagePackage.getBodyAs(BotClientConfig.class);
            updateConfig(config);
        }

        if (messagePackage.getMessageType() == MessagePackage.Type.ACCOUNT_ASSIGNMENT_CHANGE){
            RSAccount account = messagePackage.getBodyAs(RSAccount.class);
            updateAccount(account);
        }

        handleMessage(messagePackage);
    }

    public MessageResponse send(MessagePackage messagePackage){
        MessageResponse response = new MessageResponse();
        messagePackage.setResponseKey(UUID.randomUUID().toString());
        AcuityWSClient.getInstance().getResponseTracker().getCache().put(messagePackage.getResponseKey(), response);
        AcuityWSClient.getInstance().send(messagePackage);
        return response;
    }

    public Optional<String> decryptString(EncryptedString string){
        MessageResponse response = send(new MessagePackage(MessagePackage.Type.DECRYPT_STING, MessagePackage.SERVER)
                .setBody(0, string)
                .setBody(1, acuityPassword)
        );
        return response.waitForResponse(15, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(String.class));
    }

    public abstract void onGoodLogin();

    public abstract void onBadLogin();

    public abstract void updateAccount(RSAccount rsAccount);

    public abstract void updateConfig(BotClientConfig config);

    public abstract void handleMessage(MessagePackage messagePackage);

    private void sendMachineInfo(){
        MessagePackage messagePackage = new MessagePackage(MessagePackage.Type.MACHINE_INFO, MessagePackage.SERVER).setBody(MachineUtil.buildMachineState());
        send(messagePackage);
    }

    public static void main(String[] args) {
        AbstractBotController abstractBotController = new AbstractBotController("localhost", ClientType.DREAMBOT.getID()) {
            @Override
            public void onGoodLogin() {

            }

            @Override
            public void onBadLogin() {

            }

            @Override
            public void updateAccount(RSAccount rsAccount) {
                if (rsAccount != null) System.out.println(decryptString(rsAccount.getPassword()));
            }

            @Override
            public void updateConfig(BotClientConfig config) {

            }

            @Override
            public void handleMessage(MessagePackage messagePackage) {

            }
        };

        try {
            abstractBotController.start(PasswordStore.getAcuityEmail(), PasswordStore.getAcuityPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
