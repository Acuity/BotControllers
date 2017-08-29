package com.acuity.control.client.accounts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class RSAccountManager {

    private BotControl botControl;
    private RSAccount rsAccount;

    public RSAccountManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void onLoop(){
        Pair<ScriptExecutionConfig, Object> scriptInstance = botControl.getScriptManager().getScriptInstance();
        if (rsAccount == null && scriptInstance != null && scriptInstance.getKey().getScriptRunConfig().getPullAccountsFromTagID() != null){
            requestAccountFromTag(scriptInstance.getKey().getScriptRunConfig().getPullAccountsFromTagID(), false);
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

    public boolean requestAccountFromTag(String pullID, boolean force){
        List<RSAccount> rsAccounts = botControl.getRSAccounts();
        Collections.shuffle(rsAccounts);
        for (RSAccount account : rsAccounts) {
            if (account.getTagIDs().contains(pullID) && !botControl.isAccountAssigned(account)){
                if (botControl.requestAccountAssignment(account, force)) return true;
            }
        }
        return false;
    }

    public RSAccount getRsAccount() {
        return rsAccount;
    }

    public void onRSAccountAssignmentUpdate(RSAccount account) {
        this.rsAccount = account;
    }

    public void onBannedAccount() {

    }

    public void onLockedAccount() {

    }

    public void onWrongLogin() {

    }
}
