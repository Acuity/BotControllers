package com.acuity.control.client.network.endpoints;

import com.acuity.control.client.network.BotControlConnection;
import com.acuity.control.client.network.ControlEndpoint;
import com.acuity.control.client.util.MachineUtil;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by Zachary Herridge on 10/9/2017.
 */
public class RestartClientEP extends ControlEndpoint {

    private static Logger logger = LoggerFactory.getLogger(RestartClientEP.class);

    @Override
    public boolean isEndpointOf(int i) {
        return MessagePackage.Type.RESTART_CLIENT_APPLICATION == i;
    }

    @Override
    public void handle(BotControlConnection botControlConnection, MessagePackage messagePackage) {
        try {
            MachineUtil.restartApplication(null);
        } catch (IOException e) {
            logger.error("Error during restarting application.", e);
        }
    }
}
