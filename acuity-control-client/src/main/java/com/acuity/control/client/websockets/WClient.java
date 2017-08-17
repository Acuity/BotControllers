package com.acuity.control.client.websockets;

import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.util.Json;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Zach on 8/5/2017.
 */
public class WClient extends WebSocketClient implements SubscriberExceptionHandler{

    private static final Logger logger = LoggerFactory.getLogger(WClient.class);

    private EventBus eventBus = new EventBus(this);

    public WClient(String serverURL, Draft draft) throws URISyntaxException {
        super(new URI(serverURL), draft);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        eventBus.post(new WClientEvent.Opened());
    }

    @Override
    public void onMessage(String message) {
        MessagePackage messagePackage = Json.GSON.fromJson(message, MessagePackage.class);
        if (messagePackage != null) eventBus.post(messagePackage);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        eventBus.post(new WClientEvent.Closed(code, reason, remote));
    }

    @Override
    public void onError(Exception e) {
        eventBus.post(new WClientEvent.Error(e, null));
    }

    @Override
    public void handleException(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext) {
        eventBus.post(new WClientEvent.Error(throwable, subscriberExceptionContext));
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
