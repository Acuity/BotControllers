package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.AcuityWSClient;
import com.acuity.control.client.breaks.BreakHandler;
import com.acuity.ui.LoginFrame;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.PaintListener;

import java.awt.*;

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

    @Override
    public void onExit() {
        dreambotController.stop();
    }

    public static void main(String[] args) {
        new DreambotControlScript().onStart();
    }
}
