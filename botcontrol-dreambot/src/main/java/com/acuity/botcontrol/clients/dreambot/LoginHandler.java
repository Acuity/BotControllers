package com.acuity.botcontrol.clients.dreambot;

import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.google.common.eventbus.Subscribe;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.utilities.Timer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

public class LoginHandler {

	private final Timer timer = new Timer();

    private DreambotControlScript dreambotControlScript;

    public LoginHandler(DreambotControlScript dreambotControlScript) {
        this.dreambotControlScript = dreambotControlScript;
    }

    @Subscribe
	public int onLoop() {
        RSAccount account = dreambotControlScript.getController().getAccount();
        if (account == null){
            if (dreambotControlScript.getClient().isLoggedIn()){
                dreambotControlScript.getTabs().logout();
                return 1000;
            }
        }
        else if (dreambotControlScript.getClient().getGameStateID() < 25){
            switch (dreambotControlScript.getClient().getLoginIndex()) {
                case 2:
                    switch (dreambotControlScript.getClient().getLoginResponse()) {
                        case TOO_MANY_ATTEMPTS:
                            dreambotControlScript.log("Too many login attempts! Sleeping for 2 minutes.");
                            clearText();
                            timer.setRunTime(120000);
                            timer.reset();
                            break;
                        case DISABLED:
                            // TODO: 8/13/2017 Banned Account
                            break;
                        case ACCOUNT_LOCKED:
                            // TODO: 8/13/2017 Locked
                            break;
                        default:
                            dreambotControlScript.getKeyboard().type(account.getEmail());
                            dreambotControlScript.getKeyboard().type(getPassword(account));
                            dreambotControlScript.getMouse().click(new Point((int) (235 + (Math.random() * (370 - 235))), (int) (305 + (Math.random() * (335 - 305)))));
                            dreambotControlScript.sleepUntil(() -> dreambotControlScript.getClient().getGameStateID() >= 25, TimeUnit.SECONDS.toMillis(15));
                            break;
                    }
                case 3:
                    // TODO: 8/13/2017 Wrong login
                    dreambotControlScript.getMouse().click(new Point(462, 290));
                    break;
                default:
                    dreambotControlScript.getMouse().click(new Point(462, 290));
                    break;
            }
            return 1000;
        }

		return 0;
	}

	private String getPassword(RSAccount rsAccount){
        return dreambotControlScript.getController().decryptString(rsAccount.getPassword()).orElse("");
    }

	private void clearText() {
		while (!dreambotControlScript.getClient().getUsername().equals("")) {
            dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_BACK_SPACE);
		}
		if (!dreambotControlScript.getClient().getPassword().equals("")) {
            dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_TAB);
			while (!dreambotControlScript.getClient().getPassword().equals("")) {
                dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_BACK_SPACE);
			}
            dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_TAB);
		}
	}

}