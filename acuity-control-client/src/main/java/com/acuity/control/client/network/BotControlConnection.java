package com.acuity.control.client.network;

import com.acuity.common.security.PasswordStore;
import com.acuity.common.ui.LoginFrame;
import com.acuity.common.util.PackageReflectionUtil;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.network.netty.NettyClient;
import com.acuity.control.client.network.netty.TestNettyClient;
import com.acuity.control.client.network.response.MessageResponse;
import com.acuity.control.client.util.MachineUtil;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.common.EncryptedString;
import com.acuity.db.domain.vertex.impl.message_package.MessageEndpoint;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.google.common.eventbus.Subscribe;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.Permission;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class BotControlConnection {

    private static final Logger logger = LoggerFactory.getLogger(BotControlConnection.class);
    private static final Permission DECRYPT_STRING_PERMISSION = new RuntimePermission("decryptString");
    private static final Set<MessageEndpoint> endpoints = PackageReflectionUtil.findAndInit(MessageEndpoint.class, "com.acuity.control.client.network.endpoints");

    private final Object lock = new Object();

    private BotControl botControl;
    private String host;
    private String acuityEmail;
    private char[] acuityPassword;
    private int botTypeID;

    private LoginFrame loginFrame;

    private NetworkInterface networkInterface = new TestNettyClient();

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
        networkInterface.getEventBus().register(this);
        networkInterface.start();
    }

    public void stop(){
        try {
            networkInterface.getEventBus().unregister(this);
        }catch (IllegalArgumentException ignored){
        }
        networkInterface.shutdown();
    }

    @Subscribe
    public void onConnect(NetworkEvent.Opened opened){
        synchronized (lock){
            if (acuityEmail == null || acuityPassword == null) networkInterface.disconnect();

            Boolean result = send(new MessagePackage(MessagePackage.Type.LOGIN, null)
                    .setBody(0, acuityEmail)
                    .setBody(1, new String(acuityPassword))
            ).waitForResponse(30, TimeUnit.SECONDS).getResponse()
                    .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                    .orElse(false);

            if (!result) networkInterface.disconnect();
            else {
                result = send(new MessagePackage(MessagePackage.Type.BOT_CLIENT_HANDSHAKE, MessagePackage.SERVER)
                        .setBody(0, botControl.getBotClientConfig())
                        .setBody(1, botTypeID)
                ).waitForResponse(30, TimeUnit.SECONDS).getResponse()
                        .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                        .orElse(false);

                if (!result) networkInterface.disconnect();
                else {
                   // networkInterface.send(new MessagePackage(MessagePackage.Type.MACHINE_INFO, MessagePackage.SERVER).setBody(MachineUtil.buildMachineState()));
                }
            }
        }
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
        networkInterface.getResponseTracker().getCache().put(messagePackage.getResponseKey(), response);
        networkInterface.send(messagePackage);
        logger.debug("Sent - {}.", messagePackage);
        return response;
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public BotControl getBotControl() {
        return botControl;
    }

    @SuppressWarnings("unchecked")
    @Subscribe
    public void onMessage(MessagePackage messagePackage){
        for (MessageEndpoint endpoint : endpoints) {
            if (endpoint.isEndpointOf(messagePackage.getMessageType())) {
                try {
                    endpoint.handle(this, messagePackage);
                    return;
                }
                catch (Throwable e){
                    logger.error("Error during endpoint handling.", e);
                }
            }
        }
        botControl.getEventBus().post(messagePackage);
    }

    public void sendScreenCapture(Integer scale) {
        BufferedImage screenCapture = botControl.getScreenCapture();

        if (screenCapture == null) return;

        if(scale != 0){
            try {
                screenCapture = Thumbnails.of(screenCapture)
                        .size(screenCapture.getWidth() / (2 * scale), screenCapture.getHeight() / (2 * scale))
                        .outputFormat("jpg")
                        .asBufferedImage();
            } catch (Throwable e) {
                logger.error("Error during scaling screen capture.", e);
            }
        }

        if (screenCapture != null){
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                ImageIO.write(screenCapture, "jpg", baos );
                baos.flush();
                byte[] imageBytes = baos.toByteArray();
                send(new MessagePackage(MessagePackage.Type.REQUEST_SCREEN_CAP, MessagePackage.SERVER).setBody(imageBytes));
            }
            catch (Throwable e) {
                logger.error("Error during sending screen capture.", e);
            }
        }
    }

    public boolean isConnected() {
        return networkInterface != null && networkInterface.isConnected();
    }
}
