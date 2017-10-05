package com.acuity.control.client.managers.accounts;

import com.acuity.common.account_creator.AccountCreationJobV2;
import com.acuity.common.account_creator.AccountInfoGenerator;
import com.acuity.common.util.IPUtil;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

    public Optional<RSAccount> addRSAccount(String email, String ign, String password, String creationIP) {
        return addRSAccount(email, ign, password, creationIP, null);
    }

    public Optional<RSAccount> addRSAccount(String email, String ign, String password, String creationIP, String tagID) {
        return botControl.getConnection().sendWithCredentials(new MessagePackage(MessagePackage.Type.ADD_RS_ACCOUNT, MessagePackage.SERVER)
                .setBody(2, email)
                .setBody(3, ign)
                .setBody(4, password)

                .setBody(5, creationIP)
                .setBody(6, tagID)
        ).waitForResponse(30, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(RSAccount.class));
    }

    public Optional<String> get2CaptchaKey() {
        return botControl.getConnection().send(new MessagePackage(MessagePackage.Type.REQUEST_2CAPTCHA_KEY, MessagePackage.SERVER))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse().map(messagePackage -> messagePackage.getBodyAs(String.class));
    }

    public void clearRSAccount() {
        botControl.getRemote().requestAccountAssignment(null, true);
        this.rsAccount = null;
    }

    public synchronized RSAccount requestAccountFromTag(String tagID, boolean filterUnassignable, boolean force, boolean registerNewOnFail) {
        logger.debug("Requesting account - {}, {}, {}.", rsAccount, tagID, force);
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
            String apiKey = get2CaptchaKey().orElse(null);
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
                        boolean added = addRSAccount(randomEmail, randomDisplayName, randomPassword, IPUtil.getIP().orElse(null), tagID).isPresent();
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
