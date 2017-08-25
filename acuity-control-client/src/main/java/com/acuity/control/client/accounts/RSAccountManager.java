package com.acuity.control.client.accounts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;

import java.util.Collections;
import java.util.List;

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
            String pullID = scriptInstance.getKey().getScriptRunConfig().getPullAccountsFromTagID();
            List<RSAccount> rsAccounts = botControl.getRSAccounts();
            Collections.shuffle(rsAccounts);
            for (RSAccount account : rsAccounts) {
                if (account.getTagIDs().contains(pullID) && !botControl.isAccountAssigned(account)){
                    botControl.requestAccountAssignment(account);
                }
            }
        }
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
