package com.acuity.control.client.network.endpoints;

import com.acuity.control.client.network.BotControlConnection;
import com.acuity.control.client.network.ControlEndpoint;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;

/**
 * Created by Zachary Herridge on 9/28/2017.
 */
public class InGameMessageEP extends ControlEndpoint {
    @Override
    public boolean isEndpointOf(int i) {
        return MessagePackage.Type.SEND_IN_GAME_MESSAGE == i;
    }

    @Override
    public void handle(BotControlConnection botControlConnection, MessagePackage messagePackage) {
        botControlConnection.getBotControl().sendInGameMessage(messagePackage.getBodyAs(String.class));
    }
}
