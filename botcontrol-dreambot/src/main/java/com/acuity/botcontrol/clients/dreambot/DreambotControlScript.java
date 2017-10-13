package com.acuity.botcontrol.clients.dreambot;

import com.acuity.botcontrol.clients.dreambot.control.DreambotClientInterface;
import com.acuity.botcontrol.clients.dreambot.control.DreambotExperienceTracker;
import com.acuity.botcontrol.clients.dreambot.control.DreambotItemTracker;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.managers.scripts.ScriptInstance;
import com.acuity.control.client.util.ControlUtil;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
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
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zach on 8/12/2017.
 */
@ScriptManifest(name = "Acuity Bot Controller", author = "AcuityBotting", category = Category.MISC, description = "Connects your clients to AcuityBotting.com and allows remote control/monitoring.", version = 0)
public class DreambotControlScript extends AbstractScript implements InventoryListener, AdvancedMessageListener{

    private static final Logger logger = LoggerFactory.getLogger(DreambotControlScript.class);

    private BotControl botControl = new BotControl(ControlUtil.HOST, ClientType.DREAMBOT, new DreambotClientInterface(this));

    private static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(1);

    private LoginHandler loginHandler = new LoginHandler(this);
    private DreambotItemTracker itemTracker = new DreambotItemTracker(this);
    private DreambotExperienceTracker experienceTracker = new DreambotExperienceTracker(this);

    @Override
    public void onStart() {
        botControl.getExecutorManager().getGeneral().execute(() -> {
            try {
                confirmVersion();
            } catch (Throwable e) {
                logger.error("Error during confirming version.", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void confirmVersion() throws IOException {
        Map<String, Object> dreambotController = (Map<String, Object>) ControlUtil.getGlobalInfoDoc().getOrDefault("dreambotController", Collections.EMPTY_MAP);

        String remoteVersion = (String) dreambotController.get("version");
        String localVersion = getControllerVersion();

        logger.info("Comparing versions. {}, {}", localVersion, remoteVersion);
        if (!localVersion.equalsIgnoreCase(remoteVersion)){
            logger.info("Update required.");
        }
        else {
            logger.info("No update required, starting script.");
        }
    }

    public String getControllerVersion(){
        return "1.1.1";
    }

    @Override
    public int onLoop() {
        if (!botControl.getConnection().isConnected()) return DEFAULT_TIMEOUT;

        int result = botControl.getBreakManager().onLoop();
        if (result > 0) return result;

        experienceTracker.execute();

        if (!botControl.getClientInterface().isSignedIn()) return DEFAULT_TIMEOUT;
        if (botControl.getWorldManager().onLoop()) return  DEFAULT_TIMEOUT;

        ScriptInstance dreambotScript = botControl.getScriptManager().getExecutionInstance().orElse(null);
        if (dreambotScript != null) {
            Object instance = dreambotScript.getInstance();
            if (instance == null){
                logger.info("Instance null. {}", dreambotScript);
                dreambotScript.setInstance(botControl.getClientInterface().createInstanceOfScript(dreambotScript.getScriptNode()));
            }
            else {
                try {
                    int scriptSleep = ((AbstractScript) instance).onLoop();
                    if (scriptSleep < 0) botControl.getScriptManager().onScriptEnded(dreambotScript);
                    return Math.max(scriptSleep, DEFAULT_TIMEOUT);
                }
                catch (Throwable e){
                    logger.error("Error during " + instance + " script-loop.", e);
                }
            }
        }

        return DEFAULT_TIMEOUT;
    }

    @Override
    public void onPaint(Graphics graphics) {
        super.onPaint(graphics);
        ScriptInstance scriptInstance = botControl.getScriptManager().getExecutionInstance().orElse(null);
        if (scriptInstance != null && scriptInstance.getInstance() != null) {
            try {
                ((AbstractScript) scriptInstance.getInstance()).onPaint(graphics);
            }
            catch (Throwable e){
                logger.error("Error during " + scriptInstance + " paint.", e);
            }
        }
    }

    @Override
    public void onExit() {
        botControl.stop();
    }

    public BotControl getBotControl() {
        return botControl;
    }

    @Override
    public void onItemChange(Item[] items) {
        try {
            itemTracker.onUpdate();
            for (Item item : items) {
                itemTracker.onChange(item);
            }
        }
        catch (Throwable e){
            logger.error("Error during ItemTracker updates.", e);
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

    public LoginHandler getLoginHandler() {
        return loginHandler;
    }

    private void sendInGameMessage(Message message){
        botControl.getRemote().send(new MessagePackage(MessagePackage.Type.IN_GAME_MESSAGE, MessagePackage.SERVER)
                .setBody(0, message.getMessage())
                .setBody(1, message.getTypeID())
        );
    }
}
