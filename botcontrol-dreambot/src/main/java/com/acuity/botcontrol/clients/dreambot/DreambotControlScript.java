package com.acuity.botcontrol.clients.dreambot;

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
@ScriptManifest(name = "AcuityBotControl", author = "AcuityBotting", category = Category.MISC, description = "", version = 0)
public class DreambotControlScript extends AbstractScript implements PaintListener{

    private DreambotController dreambotController = new DreambotController(this);
    private LoginFrame loginFrame = new LoginFrame();
    private LoginHandler loginHandler = new LoginHandler(this);
    private BreakHandler breakHandler = new BreakHandler(dreambotController);
    private AbstractScript dreambotScript;

    @Override
    public void onStart() {
        loginFrame.getLoginButton().addActionListener(e -> {
            try {
                dreambotController.stop();
                dreambotController.start(loginFrame.getEmailField().getText(), loginFrame.getPasswordField().getText());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        loginFrame.setVisible(true);
    }

    public DreambotController getController() {
        return dreambotController;
    }

    @Override
    public void onPaint(Graphics graphics) {
        graphics.drawString("Script: " + dreambotController.getScript(), 100, 100);
        graphics.drawString("Account: " + dreambotController.getAccount(), 100, 115);
        graphics.drawString("Proxy: " + dreambotController.getProxy(), 100, 130);
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
