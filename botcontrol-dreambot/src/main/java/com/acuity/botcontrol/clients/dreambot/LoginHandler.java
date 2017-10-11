package com.acuity.botcontrol.clients.dreambot;

import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import org.dreambot.api.methods.MethodProvider;
import org.dreambot.api.utilities.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LoginHandler {

    private static final Point EXISTING_USER = new Point(458, 292);
    private static final Point CANCEL = new Point(462, 326);
    private static final Point LOGIN = new Point(292, 326);
    private static final Point TRY_AGAIN = new Point(385, 278);
    private static Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    private static String lastEmail;
    private final Timer timer = new Timer();
    private DreambotControlScript dreambotControlScript;


    public LoginHandler(DreambotControlScript dreambotControlScript) {
        this.dreambotControlScript = dreambotControlScript;
    }

    public boolean execute() {
        RSAccount account = dreambotControlScript.getBotControl().getRsAccountManager().getRsAccount();
        switch (dreambotControlScript.getClient().getLoginIndex()) {
            case 2:
                switch (dreambotControlScript.getClient().getLoginResponse()) {
                    case TOO_MANY_ATTEMPTS:
                        logger.warn("Too many login attempts! Sleeping for 2 minutes.");
                        timer.setRunTime(120000);
                        timer.reset();
                        break;
                    case DISABLED:
                        if (!Objects.equals(lastEmail, account.getEmail())) {
                            clearText();
                            return true;
                        }
                        dreambotControlScript.getBotControl().getRsAccountManager().onBannedAccount(lastEmail, account);
                        break;
                    case UPDATED:
                    case SERVER_UPDATED:
                        dreambotControlScript.getBotControl().onRunescapeUpdated();
                        break;
                    case MEMBERS_WORLD:
                        dreambotControlScript.getWorldHopper().hopWorld(308);
                        break;
                    case ACCOUNT_LOCKED:
                        if (!Objects.equals(lastEmail, account.getEmail())) {
                            clearText();
                            return true;
                        }
                        dreambotControlScript.getBotControl().getRsAccountManager().onLockedAccount(lastEmail, account);
                        break;
                    default:
                        try {
                            String password = getPassword(account);
                            if (!isLoginInfoCorrect(account.getEmail(), password)) clearText();
                            if (isTextEmpty()) {
                                dreambotControlScript.getKeyboard().type(account.getEmail());
                                dreambotControlScript.getKeyboard().type(getPassword(account));
                                MethodProvider.sleepUntil(() -> isLoginInfoCorrect(account.getEmail(), password), 10000);
                            }
                            if (isLoginInfoCorrect(account.getEmail(), password)) {
                                lastEmail = account.getEmail();
                                dreambotControlScript.getMouse().click(new Point((int) (235 + (Math.random() * (370 - 235))), (int) (305 + (Math.random() * (335 - 305)))));
                                MethodProvider.sleepUntil(() -> dreambotControlScript.getClient().getGameStateID() >= 25, TimeUnit.SECONDS.toMillis(15));
                            }
                        } catch (Throwable e) {
                            logger.error("Error during entering login.", e);
                            dreambotControlScript.getBotControl().getRsAccountManager().clearRSAccount();
                        }
                    }
                    break;
            case 3:
                if (!Objects.equals(lastEmail, account.getEmail())) {
                    clearText();
                    return true;
                }
                dreambotControlScript.getBotControl().getRsAccountManager().onWrongLogin(lastEmail, account);
                dreambotControlScript.getMouse().click(new Point(379, 273));
                break;
            default:
                dreambotControlScript.getMouse().click(new Point(462, 290));
                break;
        }
        return true;
    }

    private String getPassword(RSAccount rsAccount) {
        return dreambotControlScript.getBotControl().getConnection().decryptString(rsAccount.getPassword()).orElse("");
    }

    private boolean isLoginInfoCorrect(String username, String password) {
        return Objects.equals(username, dreambotControlScript.getClient().getUsername()) && Objects.equals(dreambotControlScript.getClient().getPassword(), password);
    }

    private boolean isTextEmpty() {
        return dreambotControlScript.getClient().getUsername().isEmpty() && dreambotControlScript.getClient().getPassword().isEmpty();
    }

    private void clearText() {
        for (int attempt = 0; attempt < 10; attempt++) {
            if (isTextEmpty()) break;
            switch (dreambotControlScript.getClient().getLoginIndex()) {
                case 3:
                    dreambotControlScript.getMouse().click(TRY_AGAIN);
                    MethodProvider.sleep(800, 1200);
                    dreambotControlScript.getMouse().click(CANCEL);
                    break;
                case 2:
                    dreambotControlScript.getMouse().click(CANCEL);
                    MethodProvider.sleep(800, 1200);
                    break;
                default:
                    dreambotControlScript.getMouse().click(EXISTING_USER);
                    MethodProvider.sleep(800, 1200);
                    break;
            }
        }
    }
}