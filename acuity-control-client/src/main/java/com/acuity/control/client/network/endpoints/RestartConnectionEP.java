package com.acuity.control.client.network.endpoints;

import com.acuity.control.client.network.BotControlConnection;
import com.acuity.control.client.network.ControlEndpoint;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Zachary Herridge on 9/28/2017.
 */
public class RestartConnectionEP extends ControlEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RestartConnectionEP.class);

    @Override
    public boolean isEndpointOf(int messageType) {
        return MessagePackage.Type.RESTART_CLIENT_CONNECTION == messageType;
    }

    @Override
    public void handle(BotControlConnection botControlConnection, MessagePackage messagePackage) {
        logger.debug("Received restart client connection command from server.");
        botControlConnection.getConnectionInterface().disconnect();
    }
}
