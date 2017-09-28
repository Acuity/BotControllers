/*
package com.acuity.control.client.network.websockets;

import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.util.Json;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

*/
/**
 * Created by Zach on 8/5/2017.
 *//*

public abstract class WClient extends WebSocketClient implements SubscriberExceptionHandler{

    private static final Logger logger = LoggerFactory.getLogger(WClient.class);

    public WClient(String serverURL, Draft draft) throws URISyntaxException {
        super(new URI(serverURL), draft);
    }

    @Override
    public void onMessage(String message) {
        MessagePackage messagePackage = Json.GSON.fromJson(message, MessagePackage.class);
        if (messagePackage != null) onMessagePackage(messagePackage);
    }

    @Override
    public abstract void onOpen(ServerHandshake serverHandshake);

    public abstract void onMessagePackage(MessagePackage messagePackage);

    @Override
    public abstract void onClose(int code, String reason, boolean remote);

    @Override
    public abstract void onError(Exception e);

    @Override
    public abstract void handleException(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext);
}
*/
