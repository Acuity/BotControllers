package com.acuity.botcontrol.machine;

import com.acuity.common.security.PasswordStore;
import com.acuity.common.ui.LoginFrame;
import com.acuity.control.client.network.netty.NettyClient;
import com.acuity.control.client.network.websockets.NetworkEvent;
import com.acuity.control.client.util.MachineUtil;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Created by Zachary Herridge on 8/24/2017.
 */
public class MachineConnection {

    private NettyClient wsClient = new NettyClient();
    private LoginFrame loginFrame;

    private String host;
    private String acuityEmail;
    private String acuityPassword;

    public MachineConnection(String host) {
        this.host = host;
        wsClient.getEventBus().register(this);
        handleLogin();
    }

    @Subscribe
    public void onLoginComplete(NetworkEvent.LoginComplete loginComplete){
        wsClient.send(new MessagePackage(MessagePackage.Type.MACHINE_INFO, MessagePackage.SERVER).setBody(MachineUtil.buildMachineState()));
    }

    @Subscribe
    public void onConnect(NetworkEvent.Opened event){
     /*   wsClient.send(new MessagePackage(MessagePackage.Type.LOGIN, null).setBody(
                new LoginData(acuityEmail, acuityPassword, 2, ClientType.UNKNOWN.getID())
        ));*/
    }

    public void start(String email, String password) throws Exception {
        this.acuityEmail = email;
        this.acuityPassword = password;
        wsClient.getEventBus().register(this);
        wsClient.start();
    }

    public EventBus getEventBus(){
        return wsClient.getEventBus();
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

}
