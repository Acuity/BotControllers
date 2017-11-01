package com.acuity.control.client.managers;

import com.acuity.common.util.IPUtil;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zachary Herridge on 10/11/2017.
 */
public class StateManager {

    private static final Logger logger = LoggerFactory.getLogger(StateManager.class);

    private BotControl botControl;
    private Instant lastIPGrab = Instant.MIN;
    private List<String> last10Messages = new ArrayList<>();

    public StateManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public synchronized void send() {
        Instant now = Instant.now();

        BotClientState clientState = new BotClientState();

        clientState.setLastMessages(last10Messages);

        if (lastIPGrab.isBefore(now.minusSeconds(15))){
            lastIPGrab = now;
            clientState.setIP(IPUtil.getIP().orElse(null));
        }

        try {
            clientState.setLastEmail(botControl.getClientInterface().getEmail());
            clientState.setGameState(botControl.getClientInterface().getGameState());
            clientState.setRsWorld(botControl.getClientInterface().getCurrentWorld());
        }
        catch (Throwable e){
            logger.error("Error during updating ClientState", e);
        }

        botControl.getClientInterface().updateClientState(clientState);

        clientState.setRsAccount(botControl.getRsAccountManager().getRsAccount());
        clientState.setProxy(botControl.getProxyManager().getProxy());
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

    public List<String> getLast10Messages() {
        return last10Messages;
    }

    public synchronized void receivedInGameMessage(int typeID, String message) {
        last10Messages.add(0, message);
        while (last10Messages.size() > 10) last10Messages.remove(10);
    }
}
