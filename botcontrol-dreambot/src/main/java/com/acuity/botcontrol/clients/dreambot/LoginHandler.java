package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.managers.scripts.ScriptManager;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccountSelector;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptSelector;
import com.google.common.base.Strings;
import org.dreambot.api.methods.MethodProvider;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class LoginHandler {

    private static Logger logger = LoggerFactory.getLogger(LoginHandler.class);

    private final Timer timer = new Timer();

    private DreambotControlScript dreambotControlScript;

    private static final Point EXISTING_USER = new Point(458, 292);
    private static final Point CANCEL = new Point(462, 326);
    private static final Point LOGIN = new Point(292, 326);
    private static final Point TRY_AGAIN = new Point(385, 278);

    public LoginHandler(DreambotControlScript dreambotControlScript) {
        this.dreambotControlScript = dreambotControlScript;
    }

    public boolean onLoop() {
        RSAccount account = dreambotControlScript.getBotControl().getRsAccountManager().getRsAccount();
        ScriptNode executionNode = dreambotControlScript.getBotControl().getScriptManager().getExecutionNode().orElse(null);
        RSAccountSelector rsAccountSelector = Optional.ofNullable(dreambotControlScript.getBotControl().getBotClientConfig().getScriptSelector()).map(ScriptSelector::getRsAccountSelector).orElse(null);

        if (executionNode != null && executionNode.getRsAccountSelector() != null) {
            rsAccountSelector = executionNode.getRsAccountSelector();
        }

        logger.debug("LoginHandler start. {}, {}, {}", account, executionNode, rsAccountSelector);

        if (account == null && executionNode == null) {
            if (dreambotControlScript.getClient().isLoggedIn()) {
                logger.debug("Logged into account with assignment.");
                logout();
                return true;
            }
        }

        if (account == null && rsAccountSelector != null){
            dreambotControlScript.getBotControl().getRsAccountManager().requestAccountFromTag(
                    rsAccountSelector.getAccountSelectionID(),
                    true,
                    false,
                    rsAccountSelector.isRegistrationAllowed());
            return true;
        }

        if (account != null && executionNode == null) {
            synchronized (ScriptManager.LOCK){
                if (dreambotControlScript.getBotControl().getScriptManager().getExecutionNode() == null){
                    logger.debug("Assigned account without node.");
                    dreambotControlScript.getBotControl().getRsAccountManager().clearRSAccount();
                }
            }
            return true;
        }

        if (account != null && rsAccountSelector != null) {
            logger.debug("CHECK. {}, {}", account.getTagIDs(), rsAccountSelector.getAccountSelectionID());
            if (!account.getTagIDs().contains(rsAccountSelector.getAccountSelectionID())) {
                logger.debug("Assigned account does not contain correct id. {}, {}", account.getTagIDs(), rsAccountSelector.getAccountSelectionID());
                dreambotControlScript.getBotControl().getRsAccountManager().clearRSAccount();
                return true;
            }
        }


        if (account != null && dreambotControlScript.getClient().isLoggedIn()) {
            if (!account.getEmail().equalsIgnoreCase(dreambotControlScript.getClient().getUsername())) {
                logger.debug("Logged into wrong account.");
                logout();
                return true;
            } else {
                if (Strings.isNullOrEmpty(account.getIgn())) {
                    String ign = Optional.ofNullable(dreambotControlScript.getClient().getLocalPlayer()).map(Player::getName).orElse(null);
                    if (ign != null) {
                        dreambotControlScript.getBotControl().send(new MessagePackage(MessagePackage.Type.SEND_IGN, MessagePackage.SERVER)
                                .setBody(0, ign)
                                .setBody(1, account.getEmail())
                        );
                    }
                }
                return false;
            }
        }

        if (account != null && dreambotControlScript.getClient().getGameStateID() < 25) {
            switch (dreambotControlScript.getClient().getLoginIndex()) {
                case 2:
                    switch (dreambotControlScript.getClient().getLoginResponse()) {
                        case TOO_MANY_ATTEMPTS:
                            logger.warn("Too many login attempts! Sleeping for 2 minutes.");
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
                            String password = getPassword(account);
                            if (!isLoginInfoCorrect(account.getEmail(), password)) clearText();
                            if (isTextEmpty()){
                                dreambotControlScript.getKeyboard().type(account.getEmail());
                                dreambotControlScript.getKeyboard().type(getPassword(account));
                                MethodProvider.sleepUntil(() -> isLoginInfoCorrect(account.getEmail(), password), 10000);
                            }
                            if (isLoginInfoCorrect(account.getEmail(), password)){
                                dreambotControlScript.getMouse().click(new Point((int) (235 + (Math.random() * (370 - 235))), (int) (305 + (Math.random() * (335 - 305)))));
                                MethodProvider.sleepUntil(() -> dreambotControlScript.getClient().getGameStateID() >= 25, TimeUnit.SECONDS.toMillis(15));
                            }
                            break;
                    }
                    break;
                case 3:
                    dreambotControlScript.getBotControl().getRsAccountManager().onWrongLogin(account);
                    dreambotControlScript.getMouse().click(new Point(379, 273));
                    break;
                default:
                    dreambotControlScript.getMouse().click(new Point(462, 290));
                    break;
            }
            return true;
        }

        return false;
    }

    private String getPassword(RSAccount rsAccount) {
        return dreambotControlScript.getBotControl().getConnection().decryptString(rsAccount.getPassword()).orElse("");
    }

    private void logout() {
        logger.debug("logging out.");
        dreambotControlScript.getWalking().clickTileOnMinimap(dreambotControlScript.getLocalPlayer().getTile());
        dreambotControlScript.getTabs().logout();
    }

    private boolean isLoginInfoCorrect(String username, String password){
        return Objects.equals(username, dreambotControlScript.getClient().getUsername()) && Objects.equals(dreambotControlScript.getClient().getPassword(), password);
    }

    private boolean isTextEmpty(){
        return dreambotControlScript.getClient().getUsername().isEmpty() && dreambotControlScript.getClient().getPassword().isEmpty();
    }

    private void clearText() {
        for (int i = 0; i < 10; i++) {
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