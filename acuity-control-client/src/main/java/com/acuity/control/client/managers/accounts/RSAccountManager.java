package com.acuity.control.client.managers.accounts;

import com.acuity.common.account_creator.AccountCreationJobV2;
import com.acuity.common.account_creator.AccountInfoGenerator;
import com.acuity.common.util.IPUtil;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccountSelector;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class RSAccountManager {

    private static final Logger logger = LoggerFactory.getLogger(RSAccountManager.class);

    private BotControl botControl;

    private RSAccount rsAccount;

    private AccountInfoGenerator accountInfoGenerator = new AccountInfoGenerator();
    private int requestFailures = 0;

    public RSAccountManager(BotControl botControl) {
        this.botControl = botControl;
    }


    public boolean execute(){
        RSAccount account = botControl.getRsAccountManager().getRsAccount();
        ScriptNode executionNode = botControl.getScriptManager().getExecutionNode().orElse(null);
        RSAccountSelector rsAccountSelector = Optional.ofNullable(botControl.getBotClientConfig())
                .map(BotClientConfig::getScriptSelector)
                .map(ScriptSelector::getRsAccountSelector).orElse(null);

        if (executionNode != null && executionNode.getRsAccountSelector() != null) {
            rsAccountSelector = executionNode.getRsAccountSelector();
        }

        logger.trace("LoginHandler start. {}, {}, {}", account, executionNode, rsAccountSelector);

        if (account == null && botControl.getClientInterface().isLoggedIn()){
            logger.debug("Logged into account without assignment.");
            botControl.getClientInterface().logout();
            return true;
        }

        if (account == null && rsAccountSelector != null){
            RSAccount rsAccount = botControl.getRsAccountManager().requestAccountFromTag(
                    rsAccountSelector.getAccountSelectionID(),
                    true,
                    false,
                    rsAccountSelector.isRegistrationAllowed());

            if (rsAccount != null) botControl.getRsAccountManager().onRSAccountAssignmentUpdate(rsAccount);
            return true;
        }

        if (account != null && executionNode == null) {
            if (botControl.getScriptManager().getExecutionNode() == null){
                logger.debug("Assigned account without node.");
                botControl.getRsAccountManager().clearRSAccount();
                return true;
            }
        }

        if (account != null && rsAccountSelector != null) {
            if (account.getTagIDs() == null || !account.getTagIDs().contains(rsAccountSelector.getAccountSelectionID())) {
                logger.debug("Assigned account does not contain correct id. {}, {}", account.getTagIDs(), rsAccountSelector.getAccountSelectionID());
                botControl.getRsAccountManager().clearRSAccount();
                return true;
            }
        }


        if (botControl.getClientInterface().isLoggedIn() && !botControl.getClientInterface().isSignedIn(rsAccount)) {
            logger.debug("Logged into wrong account.");
            botControl.getClientInterface().logout();
            return true;
        }

        if (account != null && botControl.getClientInterface().getGameState() < 25) {
            return botControl.getClientInterface().executeLoginHandler();
        }

        return false;
    }

    public synchronized RSAccount requestAccountFromTag(String tagID, boolean filterUnassignable, boolean force, boolean registerNewOnFail) {
        logger.info("Requesting account - {}, {}, {}.", rsAccount, tagID, force);
        if (rsAccount != null) {
            if (rsAccount.getTagIDs().contains(tagID)) {
                return rsAccount;
            }
            clearRSAccount();
        }

        List<RSAccount> rsAccounts = botControl.getRemote().requestRSAccounts(filterUnassignable).stream()
                .filter(rsAccount -> rsAccount.getTagIDs().contains(tagID))
                .collect(Collectors.toList());

        logger.debug("Viable RS-Accounts. {}, {}", rsAccounts.size(), rsAccounts);

        Collections.shuffle(rsAccounts);
        for (RSAccount account : rsAccounts) {
            if (botControl.getRemote().requestAccountAssignment(account, force)) {
                logger.info("Account Assigned - {}.", account.getEmail());
                requestFailures = 0;
                return account;
            }
        }

        requestFailures++;

        if (requestFailures > 3 && registerNewOnFail) {
            logger.info("Registering new RS-Account.");
            String apiKey = botControl.getRemote().request2CaptchaKey().orElse(null);
            if (apiKey != null) {
                String randomEmail = accountInfoGenerator.getRandomEmail();
                String randomDisplayName = accountInfoGenerator.getRandomDisplayName();
                int randomAge = accountInfoGenerator.getRandomAge();
                String randomPassword = accountInfoGenerator.getRandomPassword();

                try {
                    boolean result = new AccountCreationJobV2()
                            .with2CaptchaKey(apiKey)
                            .withAccountInfo(randomEmail, randomDisplayName, randomAge, randomPassword)
                            .run();
                    if (result) {
                        boolean added = botControl.getRemote().addRSAccount(randomEmail, randomDisplayName, randomPassword, IPUtil.getIP().orElse(null), tagID).isPresent();
                        if (added) {
                            requestFailures = 0;
                            return requestAccountFromTag(tagID, filterUnassignable, force, false);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error during account creation.", e);
                }
            }
            else {
                logger.warn("Failed to acquire 2Captcha key.");
            }
        }

        return null;
    }

    public void clearRSAccount() {
        botControl.getRemote().requestAccountAssignment(null, true);
        this.rsAccount = null;
    }

    public AccountInfoGenerator getAccountInfoGenerator() {
        return accountInfoGenerator;
    }

    public void setAccountInfoGenerator(AccountInfoGenerator accountInfoGenerator) {
        this.accountInfoGenerator = accountInfoGenerator;
    }

    public RSAccount getRsAccount() {
        return rsAccount;
    }

    public void onRSAccountAssignmentUpdate(RSAccount account) {
        this.rsAccount = account;
    }

    public void onBannedAccount(String lastEmail, RSAccount account) {
        logger.warn("Account banned. {}, {}", lastEmail, account);
        botControl.getRemote().requestTags("Banned").forEach(tag -> botControl.getRemote().requestTagAccount(account, tag));
        clearRSAccount();
    }

    public void onLockedAccount(String lastEmail, RSAccount account) {
        logger.warn("Account locked. {}, {}", lastEmail, account);
        botControl.getRemote().requestTags("Locked").forEach(tag -> botControl.getRemote().requestTagAccount(account, tag));
        clearRSAccount();
    }

    public void onWrongLogin(String lastEmail, RSAccount account) {
        logger.warn("Account wrong login. {}, {}", lastEmail, account);
        botControl.getRemote().requestTags("Incorrect Login").forEach(tag -> botControl.getRemote().requestTagAccount(account, tag));
        clearRSAccount();
    }
}
