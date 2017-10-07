package com.acuity.control.client.network;

import com.acuity.control.client.network.response.ResponseTracker;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.google.common.eventbus.EventBus;

/**
 * Created by Zachary Herridge on 9/29/2017.
 */
public interface ConnectionInterface {
    EventBus getEventBus();

    void start(String host);

    void shutdown();

    void disconnect();

    void send(MessagePackage messagePackage);

    ResponseTracker getResponseTracker();

    boolean isConnected();
}
