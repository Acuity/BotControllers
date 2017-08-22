package com.acuity.botcontrol.clients.dreambot;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.BotControlEvent;
import com.acuity.control.client.scripts.ScriptInstance;
import com.acuity.control.client.scripts.Scripts;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;
import com.acuity.db.domain.vertex.impl.scripts.ScriptRunConfig;
import com.acuity.db.domain.vertex.impl.scripts.ScriptVersion;
import com.google.common.eventbus.Subscribe;
import org.dreambot.Boot;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.loader.NetworkLoader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Zach on 8/12/2017.
 */
@ScriptManifest(name = "Acuity Bot Controller", author = "AcuityBotting", category = Category.MISC, description = "Connects your clients to AcuityBotting.com and allows remote control/monitoring.", version = 0)
public class DreambotControlScript extends AbstractScript {

    private BotControl botControl = new BotControl("localhost", ClientType.DREAMBOT) {
        @Override
        public Object createInstanceOfScript(ScriptRunConfig scriptRunConfig) {
            return initDreambotScript(scriptRunConfig);
        }

        @Override
        public void destroyInstanceOfScript(Object scriptInstance) {
            ((AbstractScript) scriptInstance).onExit();
        }
    };

    private LoginHandler loginHandler = new LoginHandler(this);

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

        Pair<ScriptExecutionConfig, Object> dreambotScript = botControl.getScriptManager().getScriptInstance();
        if (dreambotScript != null){
            int i = ((AbstractScript) dreambotScript.getValue()).onLoop();
            if (i < 0){
                botControl.getScriptManager().onScriptEnded(dreambotScript);
                return 2000;
            }
            return i;
        }
        return 1000;
    }

    @Override
    public void onExit() {
        botControl.stop();
    }

    public BotControl getBotControl() {
        return botControl;
    }

    @Subscribe
    public void onProxyChange(BotControlEvent.ProxyUpdated proxyUpdated){
        try {
            getClient().getSocketWrapper().getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Class<? extends AbstractScript>> getRepoScripts(){
        Map<String, Class<? extends AbstractScript>> results = new HashMap<>();
        try {
            Method getAllFreeScripts = NetworkLoader.class.getDeclaredMethod("getAllFreeScripts");
            List list = (List) getAllFreeScripts.invoke(null);
            Method getAllPremiumScripts = NetworkLoader.class.getDeclaredMethod("getAllPremiumScripts");
            list.addAll((List) getAllPremiumScripts.invoke(null));
            for (Object testObject : list) {
                try {
                    for (Method method : testObject.getClass().getDeclaredMethods()) {
                        if (method.getReturnType().equals(Class.class)){
                            method.setAccessible(true);
                            Class<? extends AbstractScript> invoke = (Class<? extends AbstractScript>) method.invoke(testObject);
                            ScriptManifest annotation = invoke.getDeclaredAnnotation(ScriptManifest.class);
                            if (annotation != null){
                                results.put(annotation.name(), invoke);
                            }
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public AbstractScript initDreambotScript(ScriptRunConfig runConfig){
        if (runConfig != null) {
            String[] args = runConfig.getQuickStartArgs() == null ? new String[0] : runConfig.getQuickStartArgs().toArray(new String[runConfig.getQuickStartArgs().size()]);
            if (runConfig.getScriptVersion().getType() == ScriptVersion.Type.ACUITY_REPO) {
                try {
                    ScriptInstance scriptInstance = Scripts.loadScript(runConfig);
                    scriptInstance.loadJar();
                    Class result = scriptInstance.getScriptLoader().getLoadedClasses().values().stream().filter(aClass -> {
                        Class superclass = aClass.getSuperclass();
                        if (superclass != null && superclass.equals(AbstractScript.class)) {
                            return true;
                        }
                        return false;
                    }).findAny().orElse(null);

                    if (result != null) {
                        try {
                            AbstractScript abstractScript = (AbstractScript) result.newInstance();
                            abstractScript.registerMethodContext(getClient());
                            abstractScript.registerContext(getClient());
                            if (args.length > 0) abstractScript.onStart(args);
                            else abstractScript.onStart();
                            return abstractScript;
                        } catch (InstantiationException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Map<String, Class<? extends AbstractScript>> repoScripts = DreambotControlScript.getRepoScripts();
                Class<? extends AbstractScript> aClass = repoScripts.get(runConfig.getScript().getTitle());
                if (aClass != null) {
                    try {
                        AbstractScript abstractScript = aClass.newInstance();
                        abstractScript.registerMethodContext(getClient());
                        abstractScript.registerContext(getClient());
                        if (args.length > 0) abstractScript.onStart(args);
                        else abstractScript.onStart();
                        return abstractScript;
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        new Boot();
        Boot.main(new String[]{});
    }
}
