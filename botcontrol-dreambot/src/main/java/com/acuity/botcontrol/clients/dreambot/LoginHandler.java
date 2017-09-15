package com.acuity.botcontrol.clients.dreambot;

import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import org.dreambot.api.methods.MethodProvider;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.Player;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class LoginHandler {

	private final Timer timer = new Timer();

	private DreambotControlScript dreambotControlScript;

	public LoginHandler(DreambotControlScript dreambotControlScript) {
		this.dreambotControlScript = dreambotControlScript;
	}

	@Subscribe
	public int onLoop() {
		RSAccount account = dreambotControlScript.getBotControl().getRsAccountManager().getRsAccount();

		if (account != null && Strings.isNullOrEmpty(account.getIgn()) && dreambotControlScript.getClient().isLoggedIn()){
			String ign = Optional.ofNullable(dreambotControlScript.getClient().getLocalPlayer()).map(Player::getName).orElse(null);
			if (ign != null) {
				dreambotControlScript.getBotControl().send(new MessagePackage(MessagePackage.Type.SEND_IGN, MessagePackage.SERVER)
						.setBody(0, ign)
						.setBody(1, account.getEmail())
				);
			}
		}

		if (account != null && dreambotControlScript.getClient().getGameStateID() < 25) {
			System.out.println(dreambotControlScript.getClient().getLoginIndex());
			switch (dreambotControlScript.getClient().getLoginIndex()) {
				case 2:
					switch (dreambotControlScript.getClient().getLoginResponse()) {
						case TOO_MANY_ATTEMPTS:
							MethodProvider.log("Too many login attempts! Sleeping for 2 minutes.");
							clearText(true);
							timer.setRunTime(120000);
							timer.reset();
							break;
						case DISABLED:
							dreambotControlScript.getBotControl().getRsAccountManager().onBannedAccount(account);
							break;
						case ACCOUNT_LOCKED:
							dreambotControlScript.getBotControl().getRsAccountManager().onLockedAccount(account);
							break;
						default:
							dreambotControlScript.getKeyboard().type(account.getEmail());
							dreambotControlScript.getKeyboard().type(getPassword(account));
							dreambotControlScript.getMouse().click(new Point((int) (235 + (Math.random() * (370 - 235))), (int) (305 + (Math.random() * (335 - 305)))));
							MethodProvider.sleepUntil(() -> dreambotControlScript.getClient().getGameStateID() >= 25, TimeUnit.SECONDS.toMillis(15));
							break;
					}
					break;
				case 3:
					dreambotControlScript.getBotControl().getRsAccountManager().onWrongLogin(account);
					dreambotControlScript.getMouse().click(new Point(379, 273));
					clearText(false);
					break;
				default:
					dreambotControlScript.getMouse().click(new Point(462, 290));
					break;
			}
			return 1000;
		} else {
			if (account == null || !account.getEmail().equals(dreambotControlScript.getClient().getUsername())) {
				if (dreambotControlScript.getClient().isLoggedIn()) {
					dreambotControlScript.getWalking().clickTileOnMinimap(dreambotControlScript.getLocalPlayer().getTile());
					dreambotControlScript.getTabs().logout();
					return 1000;
				}
			}
		}
		return 0;
	}

	private String getPassword(RSAccount rsAccount) {
		return dreambotControlScript.getBotControl().getConnection().decryptString(rsAccount.getPassword()).orElse("");
	}

	private void clearText(final boolean usernameStart) {
		if (!usernameStart) {
			dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_TAB);
		}

		while (dreambotControlScript.getClient().getUsername().equals("")) {
			dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_BACK_SPACE);
		}
		dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_TAB);

		while (!dreambotControlScript.getClient().getPassword().equals("")) {
			dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_BACK_SPACE);
		}
		dreambotControlScript.getKeyboard().typeSpecialKey((char) KeyEvent.VK_TAB);
	}
}