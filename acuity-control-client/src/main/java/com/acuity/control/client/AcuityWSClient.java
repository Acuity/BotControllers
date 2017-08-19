package com.acuity.control.client;

import com.acuity.control.client.websockets.WClient;
import com.acuity.control.client.websockets.WClientEvent;
import com.acuity.control.client.websockets.response.MessageResponse;
import com.acuity.control.client.websockets.response.ResponseTracker;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.util.Json;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Zach on 8/5/2017.
 */
public class AcuityWSClient {

    private static final Logger logger = LoggerFactory.getLogger(AcuityWSClient.class);

    private EventBus eventBus = new EventBus();

    private ResponseTracker responseTracker = new ResponseTracker();

    private Executor messageExecutor = Executors.newSingleThreadExecutor();
    private Executor reconnectExecutor = Executors.newSingleThreadExecutor();

    private ReconnectEvent reconnect;

    private WClient wClient;
    private String lastHost;

    public void start(String host) throws URISyntaxException {
        this.lastHost = host;
        this.reconnect = new ReconnectEvent();
        wClient = createWClient();
        wClient.setConnectionLostTimeout(5);
        wClient.connect();
    }

    public void stop(){
        reconnect = null;
        if (wClient != null) wClient.close();
    }

    public void send(MessagePackage messagePackage){
        System.out.println(messagePackage);
        wClient.send(Json.GSON.toJson(messagePackage));
    }

    public EventBus getEventBus(){
        return eventBus;
    }

    public boolean isConnected(){
        return wClient != null && wClient.isOpen();
    }

    public ResponseTracker getResponseTracker() {
        return responseTracker;
    }

    private WClient createWClient() throws URISyntaxException {
        return new WClient(this.lastHost, new Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                logger.info("Web socket opened.");
                eventBus.post(new WClientEvent.Opened());
            }

            @Override
            public void onMessagePackage(MessagePackage messagePackage) {
                logger.debug("onMessage: {}.", messagePackage);
                try {
                    if (messagePackage.getResponseKey() != null){
                        MessageResponse response = responseTracker.getCache().getIfPresent(messagePackage.getResponseKey());
                        if (response != null) {
                            response.setResponse(messagePackage);
                            responseTracker.getCache().invalidate(messagePackage.getResponseKey());
                        }
                    }
                }
                catch (Throwable e){
                    logger.error("Error during response handling.", e);
                }
                messageExecutor.execute(() -> eventBus.post(messagePackage));
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.info("Web socket closed.");
                if (reconnect != null) reconnectExecutor.execute(reconnect);
                eventBus.post(new WClientEvent.Closed(code, reason, remote));
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void handleException(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext) {
                throwable.printStackTrace();
            }
        };
    }

    private class ReconnectEvent implements Runnable{

        private long reconnectDelay = 3000;

        @Override
        public void run() {
            try {
                logger.debug("Sleeping for reconnect delay of {}ms.", reconnectDelay);
                Thread.sleep(reconnectDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                logger.info("Attempting reconnect.");
                start(lastHost);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}
