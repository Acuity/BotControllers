package com.acuity.control.client.managers;

import com.acuity.common.util.IPUtil;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Created by Zachary Herridge on 10/11/2017.
 */
public class StateManager {

    private static final Logger logger = LoggerFactory.getLogger(StateManager.class);

    private BotControl botControl;
    private Instant lastIPGrab = Instant.MIN;

    public StateManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public synchronized void send() {
        Instant now = Instant.now();

        BotClientState clientState = new BotClientState();

        if (lastIPGrab.isBefore(now.minusSeconds(15))){
            lastIPGrab = now;
            clientState.setIP(IPUtil.getIP().orElse(null));
        }

        botControl.getClientInterface().updateClientState(clientState);

        clientState.setRsAccount(botControl.getRsAccountManager().getRsAccount());
        clientState.setProxy(botControl.getProxyManager().getProxy());
        clientState.setBreakProfile(botControl.getBreakManager().getProfile());
        clientState.setBotClientConfig(botControl.getBotClientConfig());

        botControl.getScriptManager().getExecutionNode().ifPresent(scriptNode -> {
            clientState.setScriptID(scriptNode.getScriptID());
            clientState.setScriptVersionID(scriptNode.getScriptVersionID());
        });

        botControl.getRemote().updateClientStateNoResponse(clientState, false);

        logger.trace("Send client state. {}", clientState);
    }

    public StateManager clearIPGrabTimestamp(){
        lastIPGrab = Instant.MIN;
        return this;
    }
}
