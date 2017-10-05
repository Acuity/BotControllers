package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.BotControl;
import com.acuity.control.client.BotControlEvent;
import com.acuity.control.client.managers.ClientManager;
import com.acuity.control.client.managers.scripts.ScriptInstance;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.google.common.eventbus.Subscribe;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.AdvancedMessageListener;
import org.dreambot.api.script.listener.InventoryListener;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Created by Zach on 8/12/2017.
 */
@ScriptManifest(name = "Acuity Bot Controller", author = "AcuityBotting", category = Category.MISC, description = "Connects your clients to AcuityBotting.com and allows remote control/monitoring.", version = 0)
public class DreambotControlScript extends AbstractScript implements InventoryListener, AdvancedMessageListener{

    private static final Logger logger = LoggerFactory.getLogger(DreambotControlScript.class);

    private BotControl botControl = new BotControl("localhost", ClientType.DREAMBOT, new ClientManager() {
        @Override
        public void sendClientState() {
            BotClientState clientState = new BotClientState();

            try {
                clientState.setGameState(getClient().getGameStateID());
                clientState.setLastEmail(getClient().getUsername());
                clientState.setCurrentWorld(getClient().getCurrentWorld());
                clientState.setLastIGN(getLocalPlayer().getName());
            }
            catch (Throwable e){
                e.printStackTrace();
            }

            clientState.setLastRSAccount(botControl.getRsAccountManager().getRsAccount());

            BotClientConfig botClientConfig = botControl.getBotClientConfig();
            if (botClientConfig != null){
                clientState.setLastConfigHash(botClientConfig.hashCode());

                botControl.getScriptManager().getExecutionNode().ifPresent(scriptNode -> {
                    clientState.setLastScriptID(scriptNode.getScriptID());
                    clientState.setLastScriptVersionID(scriptNode.getScriptVersionID());
                });
            }

            botControl.getRemote().updateClientStateNoResponse(clientState, false);
            logger.trace("Sent state.");
        }

        @Override
        public Object createInstanceOfScript(ScriptNode scriptRunConfig) {
            return DreambotScriptManager.initDreambotScript(botControl, getClient(), scriptRunConfig);
        }


        @Override
        public void destroyInstanceOfScript(Object scriptInstance) {
            ((AbstractScript) scriptInstance).onExit();
        }

        @Override
        public boolean evaluate(Object evaluator) {
            return new DreambotEvaluator(DreambotControlScript.this).evaluate(evaluator);
        }

        @Override
        public boolean isSignedIn(RSAccount rsAccount) {
            return getClient().isLoggedIn() && rsAccount.getEmail().equalsIgnoreCase(getClient().getUsername());
        }

        @Override
        public void sendInGameMessage(String message) {
            getKeyboard().type(message);
        }

        @Override
        public Integer getCurrentWorld() {
            return getClient().getCurrentWorld();
        }

        @Override
        public void hopToWorld(int world) {
            getWorldHopper().hopWorld(world);
        }

        @Override
        public BufferedImage getScreenCapture() {
            return getClient().getCanvasImage();
        }

        @Override
        public boolean executeLoginHandler() {
            return loginHandler.execute();
        }

    });

    private LoginHandler loginHandler = new LoginHandler(this);
    private DreambotItemTracker itemTracker = new DreambotItemTracker(this);

    @Override
    public void onStart() {
        botControl.getEventBus().register(this);
    }

    @Override
    public int onLoop() {
        if (!botControl.getConnection().isConnected()) return 1000;

        int result = botControl.getBreakManager().onLoop();
        if (result > 0) return result;

        if (!botControl.getClientInterface().isSignedIn()) return 1000;

        if (botControl.getWorldManager().onLoop()) return  1000;

        ScriptInstance dreambotScript = botControl.getScriptManager().getExecutionInstance().orElse(null);
        if (dreambotScript != null) {
            Object instance = dreambotScript.getInstance();
            if (instance == null){
                dreambotScript.setInstance(botControl.getClientInterface().createInstanceOfScript(dreambotScript.getScriptNode()));
            }
            else {
                try {
                    logger.trace("Looping script.");
                    int scriptSleep = ((AbstractScript) instance).onLoop();
                    if (scriptSleep < 0) botControl.getScriptManager().onScriptEnded(dreambotScript);
                    return Math.max(scriptSleep, 250);
                }
                catch (Throwable e){
                    logger.error("Error during scriptOnLoop", e);
                }
            }

        }
        return 1000;
    }

    @Override
    public void onPaint(Graphics graphics) {
        super.onPaint(graphics);
        ScriptInstance scriptInstance = botControl.getScriptManager().getExecutionInstance().orElse(null);
        if (scriptInstance != null && scriptInstance.getInstance() != null) ((AbstractScript) scriptInstance.getInstance()).onPaint(graphics);
    }

    @Override
    public void onExit() {
        botControl.stop();
    }

    public BotControl getBotControl() {
        return botControl;
    }

    @Subscribe
    public void onProxyChange(BotControlEvent.ProxyUpdated proxyUpdated) {
        try {
            getClient().getSocketWrapper().getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onItemChange(Item[] items) {
        itemTracker.onUpdate();
        for (Item item : items) {
            itemTracker.onChange(item);
        }
    }

    @Override
    public void onAutoMessage(Message message) {
        sendInGameMessage(message);
    }

    @Override
    public void onPrivateInfoMessage(Message message) {
        sendInGameMessage(message);
    }

    @Override
    public void onClanMessage(Message message) {
        sendInGameMessage(message);
    }

    @Override
    public void onGameMessage(Message message) {
        sendInGameMessage(message);
    }

    @Override
    public void onPlayerMessage(Message message) {
        sendInGameMessage(message);
    }

    @Override
    public void onTradeMessage(Message message) {
        sendInGameMessage(message);
    }

    @Override
    public void onPrivateInMessage(Message message) {
        sendInGameMessage(message);
    }

    @Override
    public void onPrivateOutMessage(Message message) {
    }

    private void sendInGameMessage(Message message){
        botControl.getRemote().send(new MessagePackage(MessagePackage.Type.IN_GAME_MESSAGE, MessagePackage.SERVER)
                .setBody(0, message.getMessage())
                .setBody(1, message.getTypeID())
        );
    }
}
