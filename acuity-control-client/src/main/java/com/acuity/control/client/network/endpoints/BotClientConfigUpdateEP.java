package com.acuity.control.client.network.endpoints;

import com.acuity.control.client.network.BotControlConnection;
import com.acuity.control.client.network.ControlEndpoint;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;

/**
 * Created by Zachary Herridge on 9/28/2017.
 */
public class BotClientConfigUpdateEP extends ControlEndpoint {
    @Override
    public boolean isEndpointOf(int i) {
        return MessagePackage.Type.CONFIG_UPDATE == i;
    }

    @Override
    public void handle(BotControlConnection botControlConnection, MessagePackage messagePackage) {
        BotClientConfig config = messagePackage.getBodyAs(BotClientConfig.class);
        botControlConnection.getBotControl().getClientConfigManager().setCurrentConfig(config);
    }
}
