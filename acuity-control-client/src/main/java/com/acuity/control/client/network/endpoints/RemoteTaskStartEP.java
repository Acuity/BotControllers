package com.acuity.control.client.network.endpoints;

import com.acuity.common.util.Pair;
import com.acuity.control.client.managers.config.BotClientConfigManager;
import com.acuity.control.client.managers.scripts.RemoteScriptStartCheck;
import com.acuity.control.client.managers.scripts.ScriptInstance;
import com.acuity.control.client.network.BotControlConnection;
import com.acuity.control.client.network.ControlEndpoint;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.message_package.data.RemoteScriptTask;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Zachary Herridge on 9/28/2017.
 */
public class RemoteTaskStartEP extends ControlEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RemoteTaskStartEP.class);

    @Override
    public boolean isEndpointOf(int i) {
        return MessagePackage.Type.REQUEST_REMOTE_TASK_START == i;
    }

    @Override
    public void handle(BotControlConnection botControlConnection, MessagePackage messagePackage) {
        synchronized (BotClientConfigManager.LOCK){
            logger.debug("onMessage - REQUEST_REMOTE_TASK_START");

            BotClientConfig botClientConfig = botControlConnection.getBotControl().getBotClientConfig();
            if (botClientConfig.getTaskNode() != null){
                logger.debug("Remote Task Request - Already has task.");
                botControlConnection.getBotControl().respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey())
                        .setBody(new RemoteScriptTask.StartResponse()));
                return;
            }

            ScriptInstance scriptInstance = botControlConnection.getBotControl().getScriptManager().getExecutionInstance().orElse(null);
            if (scriptInstance != null && scriptInstance.getInstance() != null && scriptInstance.getInstance() instanceof RemoteScriptStartCheck) {
                if (!((RemoteScriptStartCheck) scriptInstance.getInstance()).isAcceptingTasks()) {
                    logger.debug("Remote Task Request - Current script not accepting new tasks.");
                    botControlConnection.getBotControl().respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey())
                            .setBody(new RemoteScriptTask.StartResponse()));
                    return;
                }
            }

            RemoteScriptTask.StartRequest scriptStartRequest = messagePackage.getBodyAs(RemoteScriptTask.StartRequest.class);
            ScriptNode taskNode = scriptStartRequest.getTaskNode();
            RSAccount rsAccount = null;

            if (taskNode.getRsAccountSelector() != null) {
                String accountAssignmentTag = taskNode.getRsAccountSelector().getAccountSelectionID();
                boolean registrationEnabled = taskNode.getRsAccountSelector().isRegistrationAllowed();
                if (scriptStartRequest.isConditionalOnAccountAssignment() && accountAssignmentTag != null) {
                    logger.debug("Remote Task Request - Conditional on account assignment, requesting account.");
                    rsAccount = botControlConnection.getBotControl().getRsAccountManager().requestAccountFromTag(accountAssignmentTag, true, false, registrationEnabled);
                    logger.debug("Remote Task Request - Account assignment result. {}", rsAccount);
                }
            }

            RemoteScriptTask.StartResponse result = new RemoteScriptTask.StartResponse();
            result.setAccount(rsAccount);
            if (rsAccount != null || !scriptStartRequest.isConditionalOnAccountAssignment()) {
                logger.debug("Remote Task Request - Adding task to queue.");

                botClientConfig.setTaskNode(taskNode);
                if (!botControlConnection.getBotControl().updateClientConfig(botClientConfig, true)) {
                    logger.debug("Remote Task Request - Failed to add task to queue, clearing account.");
                    botControlConnection.getBotControl().getRsAccountManager().clearRSAccount();
                } else {
                    result.setTaskQueued(true);
                }
            }

            logger.debug("Remote Task Request - Sending result to requester. {}, {}", result, messagePackage.getSourceKey());
            botControlConnection.getBotControl().respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey()).setBody(result));
        }
    }
}
