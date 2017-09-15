package com.acuity.botcontrol.clients.dreambot;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.BotControlEvent;
import com.acuity.control.client.machine.MachineUtil;
import com.acuity.control.client.scripts.ScriptInstance;
import com.acuity.control.client.scripts.Scripts;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.Script;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;
import com.acuity.db.domain.vertex.impl.scripts.ScriptStartupConfig;
import com.acuity.db.domain.vertex.impl.scripts.ScriptVersion;
import com.acuity.db.util.ArangoDBUtil;
import com.google.common.eventbus.Subscribe;
import org.dreambot.Boot;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.InventoryListener;
import org.dreambot.api.script.loader.NetworkLoader;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.core.Instance;
import org.dreambot.core.InstancePool;
import org.dreambot.server.net.datatype.ScriptData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Zach on 8/12/2017.
 */
@ScriptManifest(name = "Acuity Bot Controller", author = "AcuityBotting", category = Category.MISC, description = "Connects your clients to AcuityBotting.com and allows remote control/monitoring.", version = 0)
public class DreambotControlScript extends AbstractScript implements InventoryListener{

    private static final Logger logger = LoggerFactory.getLogger(DreambotControlScript.class);

    private BotControl botControl = new BotControl("localhost", ClientType.DREAMBOT) {
        @Override
        public void sendClientState() {
            BotClientState clientState = new BotClientState();
            clientState.setCpuUsage(MachineUtil.getCPUUsage());
            clientState.setGameState(getClient().getGameStateID());
            send(new MessagePackage(MessagePackage.Type.CLIENT_STATE_UPDATE, MessagePackage.SERVER).setBody(clientState));
        }

        @Override
        public Object createInstanceOfScript(ScriptStartupConfig scriptRunConfig) {
            return initDreambotScript(scriptRunConfig);
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
        public BufferedImage getScreenCapture() {
            return getClient().getCanvasImage();
        }
    };

    private LoginHandler loginHandler = new LoginHandler(this);
    private DreambotItemTracker itemTracker = new DreambotItemTracker(this);

    @SuppressWarnings("unchecked")
    public static Map<String, Class<? extends AbstractScript>> getRepoScripts() {
        Map<String, Class<? extends AbstractScript>> results = new HashMap<>();
        try {
            Method getAllFreeScripts = NetworkLoader.class.getDeclaredMethod("getAllFreeScripts");
            List list = (List) getAllFreeScripts.invoke(null);
            Method getAllPremiumScripts = NetworkLoader.class.getDeclaredMethod("getAllPremiumScripts");
            list.addAll((List) getAllPremiumScripts.invoke(null));

            for (Object testObject : list) {
                try {
                    Field scriptDataField = Arrays.stream(testObject.getClass().getDeclaredFields())
                            .filter(field -> field.getType().equals(ScriptData.class))
                            .findAny().orElse(null);

                    if (scriptDataField != null) {
                        scriptDataField.setAccessible(true);
                        ScriptData scriptData = (ScriptData) scriptDataField.get(testObject);

                        Class<? extends AbstractScript> remoteClass = NetworkLoader.getRemoteClass(scriptData);
                        if (remoteClass != null) results.put(scriptData.name, remoteClass);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public void onStart() {
        botControl.getEventBus().register(this);
    }

    @Override
    public int onLoop() {
        if (!botControl.getConnection().isConnected()) return 1000;

        botControl.onLoop();

        int result = botControl.getBreakManager().onloop();
        if (result > 0) return result;

        result = loginHandler.onLoop();
        if (result > 0) return result;

        Pair<ScriptExecutionConfig, Object> dreambotScript = botControl.getScriptManager().getScriptInstance().orElse(null);
        if (dreambotScript != null) {
            int i = ((AbstractScript) dreambotScript.getValue()).onLoop();
            if (i < 0) {
                botControl.getScriptManager().onScriptEnded(dreambotScript);
                return 2000;
            }
            return i;
        }
        return 1000;
    }

    @Override
    public void onPaint(Graphics graphics) {
        super.onPaint(graphics);
        Pair<ScriptExecutionConfig, Object> scriptInstance = botControl.getScriptManager().getScriptInstance().orElse(null);
        if (scriptInstance != null) ((AbstractScript) scriptInstance.getValue()).onPaint(graphics);
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

    public AbstractScript initDreambotScript(ScriptStartupConfig runConfig) {
        if (runConfig != null) {
            logger.debug("initDreambotScript - initing off ScriptStartupConfig. {}", runConfig);
            ScriptVersion scriptVersion = botControl.requestScriptVersion(runConfig.getScriptID(), runConfig.getScriptVersionID()).orElse(null);
            if (scriptVersion != null) {
                String[] args = runConfig.getQuickStartArgs() == null ? new String[0] : runConfig.getQuickStartArgs().toArray(new String[runConfig.getQuickStartArgs().size()]);
                if (scriptVersion.getType() == ScriptVersion.Type.ACUITY_REPO) {
                    logger.debug("initDreambotScript - loading version off Acuity-Repo.", scriptVersion);
                    try {
                        ScriptInstance scriptInstance = Scripts.loadScript(
                                ArangoDBUtil.keyFromID(runConfig.getScriptID()),
                                ArangoDBUtil.keyFromID(runConfig.getScriptID()),
                                ClientType.DREAMBOT.getID(),
                                scriptVersion.getRevision(),
                                scriptVersion.getJarURL()
                                );
                        scriptInstance.loadJar();
                        Class result = scriptInstance.getScriptLoader().getLoadedClasses().values().stream().filter(AbstractScript.class::isAssignableFrom).findAny().orElse(null);
                        if (result != null) {
                            return startScript(result, args);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Script script = botControl.requestScript(runConfig.getScriptID()).orElse(null);
                    if (script != null){
                        logger.debug("initDreambotScript - loading version off Dreambot-Repo.", script);
                        Map<String, Class<? extends AbstractScript>> repoScripts = DreambotControlScript.getRepoScripts();
                        Class<? extends AbstractScript> aClass = repoScripts.get(script.getTitle());
                        if (aClass != null) {
                            return startScript(aClass, args);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void setBotControl(Class clazz, Object object) {
        Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.getType().equals(BotControl.class)).forEach(field -> {
            boolean accessible = field.isAccessible();
            if (!accessible) field.setAccessible(true);
            try {
                field.set(object, botControl);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (!accessible) field.setAccessible(false);
        });
    }

    private AbstractScript startScript(Class clazz, String[] args) {
        try {
            AbstractScript abstractScript = (AbstractScript) clazz.newInstance();
            setBotControl(clazz, abstractScript);
            setBotControl(clazz.getSuperclass(), abstractScript);
            abstractScript.registerMethodContext(getClient());
            abstractScript.registerContext(getClient());
            if (args.length > 0) abstractScript.onStart(args);
            else abstractScript.onStart();
            return abstractScript;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void onItemChange(Item[] items) {
        for (Item item : items) {
            itemTracker.onChange(item);
        }
    }

    public static void main(String[] args) {
        Boot.main(new String[]{});

        while (InstancePool.getAll().size() == 0) {
            sleep(1000);
        }

        Instance instance = InstancePool.getAll().stream().findFirst().orElse(null);
        DreambotControlScript dreambotControlScript = new DreambotControlScript();
        dreambotControlScript.registerContext(instance.getClient());
        dreambotControlScript.registerMethodContext(instance.getClient());
        dreambotControlScript.onStart();

        while (true){
            int i = dreambotControlScript.onLoop();
            sleep(i);
        }
    }
}
