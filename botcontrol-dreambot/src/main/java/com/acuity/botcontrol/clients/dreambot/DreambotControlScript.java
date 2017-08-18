package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.AcuityWSClient;
import com.acuity.control.client.breaks.BreakHandler;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.loader.NetworkLoader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

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
        if (!AcuityWSClient.getInstance().isConnected()) return 1000;

        dreambotController.onLoop();

        int result = breakHandler.loop();
        if (result > 0) return result;

        result = loginHandler.onLoop();
        if (result > 0) return result;

        return dreambotScript != null ? dreambotScript.onLoop() : 750;
    }

    public static void printRepoScripts(){
        try {
            Method getAllFreeScripts = NetworkLoader.class.getDeclaredMethod("getAllFreeScripts");
            List list = (List) getAllFreeScripts.invoke(null);
            for (Object testObject : list) {
                try {
                    System.out.println(testObject);

                    for (Method method : testObject.getClass().getDeclaredMethods()) {
                        if (method.getReturnType().equals(Class.class)){
                            System.out.println("FOUND METHOD1");
                            method.setAccessible(true);
                            Class invoke = (Class) method.invoke(testObject);
                            System.out.println("Invoke: " + invoke);

                            Object instance = invoke.newInstance();
                            if (instance instanceof AbstractScript){

                            }
                            System.out.println("Instance: " + instance);

                            for (Field field : instance.getClass().getDeclaredFields()) {
                                System.out.println(field);
                            }

                            for (Method method1 : instance.getClass().getDeclaredMethods()) {
                                System.out.println(method1);
                            }

                        }
                    }

                    System.out.println();
                }
                catch (Exception ignored){
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onExit() {
        dreambotController.stop();
    }

    public static void main(String[] args) {
        new DreambotControlScript().onStart();
    }
}
