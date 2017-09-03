package com.acuity.control.client.accounts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class RSAccountManager {

    private static final Logger logger = LoggerFactory.getLogger(RSAccountManager.class);

    private BotControl botControl;
    private RSAccount rsAccount;

    public RSAccountManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void onLoop(){
        Pair<ScriptExecutionConfig, Object> scriptInstance = botControl.getScriptManager().getScriptInstance().orElse(null);
        if (scriptInstance != null && rsAccount == null && scriptInstance.getKey().getScriptStartupConfig().getPullAccountsFromTagID() != null){
            requestAccountFromTag(scriptInstance.getKey().getScriptStartupConfig().getPullAccountsFromTagID(), false);
        }

    }

    public Optional<RSAccount> addRSAccount(String email, String ign, String password, String creationIP){
        return botControl.getConnection().sendWithCreds(new MessagePackage(MessagePackage.Type.ADD_RS_ACCOUNT, MessagePackage.SERVER)
                .setBody(2, email)
                .setBody(3, ign)
                .setBody(4, password)
                .setBody(5, creationIP)
        ).waitForResponse(30, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(RSAccount.class));
    }

    public void clearRSAccount(){
        this.rsAccount = null;
    }

    public RSAccount requestAccountFromTag(String pullID, boolean force){
        logger.debug("Requesting account - {}, {}.", pullID, force);
        List<RSAccount> rsAccounts = botControl.getRSAccounts();
        Collections.shuffle(rsAccounts);
        for (RSAccount account : rsAccounts) {
            if (account.getTagIDs().contains(pullID) && botControl.requestAccountAssignment(account, force)){
                logger.debug("Account Assigned - {}.", account.getEmail());
                return account;
            }
        }
        return null;
    }

    public RSAccount getRsAccount() {
        return rsAccount;
    }

    public void onRSAccountAssignmentUpdate(RSAccount account) {
        this.rsAccount = account;
    }

    public void onBannedAccount() {
        botControl.getTags("Banned").forEach(tag -> botControl.requestTagAccount(rsAccount, tag));
    }

    public void onLockedAccount() {
        botControl.getTags("Locked").forEach(tag -> botControl.requestTagAccount(rsAccount, tag));
    }

    public void onWrongLogin() {
        botControl.getTags("Incorrect Login").forEach(tag -> botControl.requestTagAccount(rsAccount, tag));
    }
}
