package com.acuity.control.client.network.endpoints;

import com.acuity.control.client.network.BotControlConnection;
import com.acuity.control.client.network.ControlEndpoint;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Zachary Herridge on 9/28/2017.
 */
public class AccountAssignmentEP extends ControlEndpoint {

    private static Logger logger = LoggerFactory.getLogger(AccountAssignmentEP.class);

    @Override
    public boolean isEndpointOf(int i) {
        return MessagePackage.Type.ACCOUNT_ASSIGNMENT_CHANGE == i;
    }

    @Override
    public void handle(BotControlConnection botControlConnection, MessagePackage messagePackage) {
        RSAccount account = messagePackage.getBodyAs(RSAccount.class);
        logger.debug("RSAccount assignment updated. {}", account);
        botControlConnection.getBotControl().getRsAccountManager().onRSAccountAssignmentUpdate(account);
    }
}
