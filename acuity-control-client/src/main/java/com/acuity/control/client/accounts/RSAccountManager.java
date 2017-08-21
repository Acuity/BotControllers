package com.acuity.control.client.accounts;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class RSAccountManager {

    private BotControl botControl;

    private RSAccount rsAccount;

    public RSAccountManager(BotControl botControl) {
        this.botControl = botControl;
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
