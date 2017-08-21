package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.BotControl;
import com.acuity.control.client.BotControlEvent;
import com.acuity.control.client.scripts.ScriptInstance;
import com.acuity.control.client.scripts.Scripts;
import com.acuity.db.domain.common.ClientType;
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

    private BotControl botControl = new BotControl("acuitybotting.com", ClientType.DREAMBOT);

    private LoginHandler loginHandler = new LoginHandler(this);

    private AbstractScript dreambotScript;

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

        if (dreambotScript != null){
            int i = dreambotScript.onLoop();
            if (i < 0){
                botControl.getScriptManager().onScriptEnded();
                return 2000;
            }
            return i;
        }
        return 1000;
    }

    public void setDreambotScript(AbstractScript dreambotScript) {
        this.dreambotScript = dreambotScript;
    }

    public AbstractScript getDreambotScript() {
        return dreambotScript;
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

    @Subscribe
    public void onScriptUpdate(BotControlEvent.ScriptUpdated event){
        ScriptRunConfig currentRunConfig = event.getCurrentRunConfig();
        if (currentRunConfig != null) {
            String[] args = currentRunConfig.getQuickStartArgs() == null ? new String[0] : currentRunConfig.getQuickStartArgs().toArray(new String[currentRunConfig.getQuickStartArgs().size()]);
            if (currentRunConfig.getScriptVersion().getType() == ScriptVersion.Type.ACUITY_REPO) {
                try {
                    ScriptInstance scriptInstance = Scripts.loadScript(currentRunConfig);
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
                            abstractScript.onStart(args);
                            setDreambotScript(abstractScript);
                        } catch (InstantiationException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Map<String, Class<? extends AbstractScript>> repoScripts = DreambotControlScript.getRepoScripts();
                Class<? extends AbstractScript> aClass = repoScripts.get(currentRunConfig.getScript().getTitle());
                if (aClass != null) {
                    try {
                        AbstractScript abstractScript = aClass.newInstance();
                        abstractScript.registerMethodContext(getClient());
                        abstractScript.registerContext(getClient());
                        abstractScript.onStart(args);
                        setDreambotScript(abstractScript);
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            AbstractScript abstractScript = getDreambotScript();
            setDreambotScript(null);
            if (abstractScript != null) abstractScript.onExit();
        }
    }

    public static void main(String[] args) {
        new Boot();
        Boot.main(new String[]{});
    }
}
