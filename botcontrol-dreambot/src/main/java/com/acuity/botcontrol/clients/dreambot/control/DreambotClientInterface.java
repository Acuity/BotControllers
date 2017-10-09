package com.acuity.botcontrol.clients.dreambot.control;

import com.acuity.botcontrol.clients.dreambot.DreambotControlScript;
import com.acuity.control.client.ClientInterface;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import org.dreambot.api.script.AbstractScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * Created by Zachary Herridge on 10/6/2017.
 */
public class DreambotClientInterface extends ClientInterface {

    private static final Logger logger = LoggerFactory.getLogger(DreambotClientInterface.class);

    private DreambotControlScript controlScript;

    public DreambotClientInterface(DreambotControlScript controlScript) {
        this.controlScript = controlScript;
    }

    @Override
    public void sendClientState() {
        BotClientState clientState = new BotClientState();

        try {
            clientState.setGameState(controlScript.getClient().getGameStateID());
            clientState.setLastEmail(controlScript.getClient().getUsername());
            clientState.setRsWorld(controlScript.getClient().getCurrentWorld());
            clientState.setLastIGN(controlScript.getLocalPlayer().getName());
        } catch (Throwable e) {
            logger.error("Error during state gathering.", e);
        }

        clientState.setRsAccount(controlScript.getBotControl().getRsAccountManager().getRsAccount());
        clientState.setProxy(controlScript.getBotControl().getProxyManager().getProxy());
        clientState.setBreakProfile(controlScript.getBotControl().getBreakManager().getProfile());
        clientState.setBotClientConfig(controlScript.getBotControl().getBotClientConfig());

        controlScript.getBotControl().getScriptManager().getExecutionNode().ifPresent(scriptNode -> {
            clientState.setScriptID(scriptNode.getScriptID());
            clientState.setScriptVersionID(scriptNode.getScriptVersionID());
        });

        controlScript.getBotControl().getRemote().updateClientStateNoResponse(clientState, false);

        logger.trace("Send client state. {}", clientState);
    }

    @Override
    public Object createInstanceOfScript(ScriptNode scriptRunConfig) {
        return DreambotScriptManager.initDreambotScript(controlScript.getBotControl(), controlScript.getClient(), scriptRunConfig);
    }


    @Override
    public void destroyInstanceOfScript(Object scriptInstance) {
        ((AbstractScript) scriptInstance).onExit();
    }

    @Override
    public boolean evaluate(Object evaluator) {
        return new DreambotEvaluator(controlScript).evaluate(evaluator);
    }

    @Override
    public boolean isSignedIn(RSAccount rsAccount) {
        return rsAccount != null && controlScript.getClient().isLoggedIn() && rsAccount.getEmail().equalsIgnoreCase(controlScript.getClient().getUsername());
    }

    @Override
    public void sendInGameMessage(String message) {
        controlScript.getKeyboard().type(message);
    }

    @Override
    public Integer getCurrentWorld() {
        return controlScript.getClient().getCurrentWorld();
    }

    @Override
    public void hopToWorld(int world) {
        controlScript.getWorldHopper().hopWorld(world);
    }

    @Override
    public BufferedImage getScreenCapture() {
        return controlScript.getClient().getCanvasImage();
    }

    @Override
    public boolean executeLoginHandler() {
        return controlScript.getLoginHandler().execute();
    }

    @Override
    public int getGameState() {
        return controlScript.getClient().getGameStateID();
    }

    @Override
    public void logout() {
        logger.debug("logging out.");
        try {
            controlScript.getWalking().clickTileOnMinimap(controlScript.getLocalPlayer().getTile());
        } catch (Throwable ignored) {
        }

        try {
            controlScript.getTabs().logout();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String getEmail() {
        return controlScript.getClient().getUsername();
    }
}
