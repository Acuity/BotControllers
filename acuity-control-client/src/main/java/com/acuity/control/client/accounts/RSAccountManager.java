package com.acuity.control.client.accounts;

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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class RSAccountManager {

    private static final Logger logger = LoggerFactory.getLogger(RSAccountManager.class);

    private BotControl botControl;
    private RSAccount rsAccount;

    private AccountInfoGenerator accountInfoGenerator = new AccountInfoGenerator();

    public RSAccountManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void handle(Map<String, Object> settings){
        String accountAssignmentTag = (String) settings.get("accountAssignmentTag");
        boolean registrationEnabled = (boolean) settings.get("registrationEnabled");

        if (accountAssignmentTag == null) return;

        if (!rsAccount.getTagIDs().contains(accountAssignmentTag)){
            botControl.requestAccountAssignment(null, true);
            clearRSAccount();
        }

        if (rsAccount == null){
            requestAccountFromTag(
                    accountAssignmentTag,
                    true,
                    false,
                    registrationEnabled
            );
        }
    }

    public Optional<RSAccount> addRSAccount(String email, String ign, String password, String creationIP){
        return addRSAccount(email, ign, password, creationIP, null);
    }

    public Optional<RSAccount> addRSAccount(String email, String ign, String password, String creationIP, String tagID){
        return botControl.getConnection().sendWithCredentials(new MessagePackage(MessagePackage.Type.ADD_RS_ACCOUNT, MessagePackage.SERVER)
                .setBody(2, email)
                .setBody(3, ign)
                .setBody(4, password)
                .setBody(5, creationIP)
                .setBody(6, tagID)
        ).waitForResponse(30, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(RSAccount.class));
    }

    public Optional<String> get2CaptchaKey(){
        return botControl.getConnection().send(new MessagePackage(MessagePackage.Type.REQUEST_2CAPTCHA_KEY, MessagePackage.SERVER))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse().map(messagePackage -> messagePackage.getBodyAs(String.class));
    }

    public void clearRSAccount(){
        this.rsAccount = null;
    }

    public RSAccount requestAccountFromTag(String tagID, boolean filterUnassignable, boolean force, boolean registerNewOnFail){
        logger.debug("Requesting account - {}, {}.", tagID, force);
        List<RSAccount> rsAccounts = botControl.requestRSAccounts(filterUnassignable);
        Collections.shuffle(rsAccounts);
        for (RSAccount account : rsAccounts) {
            if (account.getTagIDs().contains(tagID) && botControl.requestAccountAssignment(account, force)){
                logger.debug("Account Assigned - {}.", account.getEmail());
                return account;
            }
        }

        if (registerNewOnFail){
            String apiKey = get2CaptchaKey().orElse(null);
            if (apiKey != null){
                String randomEmail = accountInfoGenerator.getRandomEmail();
                String randomDisplayName = accountInfoGenerator.getRandomDisplayName();
                int randomAge = accountInfoGenerator.getRandomAge();
                String randomPassword = accountInfoGenerator.getRandomPassword();

                try {
                    boolean result = new AccountCreationJobV2()
                            .with2CaptchaKey(apiKey)
                            .withAccountInfo(randomEmail, randomDisplayName, randomAge, randomPassword)
                            .run();
                    if (result){
                        boolean added = addRSAccount(randomEmail, randomDisplayName, randomPassword, IPUtil.getIP().orElse(null), tagID).isPresent();
                        if (added) return requestAccountFromTag(tagID, filterUnassignable, force, registerNewOnFail);
                    }
                } catch (Exception e) {
                    logger.error("Error during account creation.", e);
                }
            }
        }
        return null;
    }

    public void setAccountInfoGenerator(AccountInfoGenerator accountInfoGenerator) {
        this.accountInfoGenerator = accountInfoGenerator;
    }

    public AccountInfoGenerator getAccountInfoGenerator() {
        return accountInfoGenerator;
    }

    public RSAccount getRsAccount() {
        return rsAccount;
    }

    public void onRSAccountAssignmentUpdate(RSAccount account) {
        this.rsAccount = account;
    }

    public void onBannedAccount(RSAccount account) {
        botControl.requestTags("Banned").forEach(tag -> botControl.requestTagAccount(account, tag));
    }

    public void onLockedAccount(RSAccount account) {
        botControl.requestTags("Locked").forEach(tag -> botControl.requestTagAccount(account, tag));
    }

    public void onWrongLogin(RSAccount account) {
        botControl.requestTags("Incorrect Login").forEach(tag -> botControl.requestTagAccount(account, tag));
    }
}
