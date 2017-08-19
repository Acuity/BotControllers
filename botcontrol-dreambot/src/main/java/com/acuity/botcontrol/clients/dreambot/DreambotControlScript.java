package com.acuity.botcontrol.clients.dreambot;

import com.acuity.common.util.Pair;
import com.acuity.control.client.AcuityWSClient;
import com.acuity.control.client.breaks.BreakHandler;
import org.dreambot.Boot;
import org.dreambot.Loader;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.loader.NetworkLoader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Zach on 8/12/2017.
 */
@ScriptManifest(name = "Acuity Bot Controller", author = "AcuityBotting", category = Category.MISC, description = "Connects your clients to AcuityBotting.com and allows remote control/monitoring.", version = 0)
public class DreambotControlScript extends AbstractScript {

    private DreambotController dreambotController = new DreambotController(this);
    private LoginHandler loginHandler = new LoginHandler(this);
    private BreakHandler breakHandler = new BreakHandler(dreambotController);
    private AbstractScript dreambotScript;

    public DreambotController getController() {
        return dreambotController;
    }

    public void setDreambotScript(AbstractScript dreambotScript) {
        this.dreambotScript = dreambotScript;
    }

    public AbstractScript getDreambotScript() {
        return dreambotScript;
    }

    public BreakHandler getBreakHandler() {
        return breakHandler;
    }

    @Override
    public int onLoop() {
        if (!dreambotController.isConnected()) return 1000;

        dreambotController.onLoop();

        int result = breakHandler.loop();
        if (result > 0) return result;

        result = loginHandler.onLoop();
        if (result > 0) return result;

        return dreambotScript != null ? dreambotScript.onLoop() : 750;
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
                            System.out.println("FOUND METHOD1");
                            method.setAccessible(true);
                            Class<? extends AbstractScript> invoke = (Class<? extends AbstractScript>) method.invoke(testObject);
                            System.out.println("Invoke: " + invoke);
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

    @Override
    public void onExit() {
        dreambotController.stop();
    }

    public static void main(String[] args) {
        new Boot();
        Boot.main(new String[]{});
    }
}
