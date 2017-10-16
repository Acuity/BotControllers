package com.acuity.control.client.network.endpoints;

import com.acuity.control.client.managers.scripts.task.TaskBlocking;
import com.acuity.control.client.managers.scripts.instance.ScriptInstance;
import com.acuity.control.client.managers.scripts.ScriptManager;
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
        synchronized (ScriptManager.LOCK){
            logger.info("RemoteTaskRequest received.");

            if (botControlConnection.getBotControl().getTaskManager().getCurrentTask() != null) {
                logger.info("Task already running.");
                botControlConnection.getBotControl().getRemote().respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey())
                        .setBody(new RemoteScriptTask.StartResponse()));
                return;
            }

            ScriptInstance scriptInstance = botControlConnection.getBotControl().getScriptManager().getExecutionInstance().orElse(null);
            if (scriptInstance != null && scriptInstance.getInstance() != null && scriptInstance.getInstance() instanceof TaskBlocking) {
                if (!((TaskBlocking) scriptInstance.getInstance()).isAcceptingTasks()) {
                    logger.info("Current executing script not accepting new tasks.");
                    botControlConnection.getBotControl().getRemote().respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey())
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
                    logger.trace("Conditional on account assignment, requesting account.");
                    rsAccount = botControlConnection.getBotControl().getRsAccountManager()
                            .requestAccountFromTag(accountAssignmentTag, true, false, registrationEnabled)
                            .orElse(null);
                    logger.trace("Account assignment result. {}", rsAccount);
                    if (rsAccount != null){
                        botControlConnection.getBotControl().getRsAccountManager().onRSAccountAssignmentUpdate(rsAccount);
                    }
                }
            }

            RemoteScriptTask.StartResponse result = new RemoteScriptTask.StartResponse();
            result.setAccount(rsAccount);
            if (rsAccount != null || !scriptStartRequest.isConditionalOnAccountAssignment()) {
                botControlConnection.getBotControl().getTaskManager().setCurrentTask(taskNode);
                result.setTaskQueued(true);
            }
            else {
                logger.info("Failed to acquire RSAccount assignment.");
            }

            logger.trace("Sending result to requester. {}, {}", result, messagePackage.getSourceKey());
            botControlConnection.getBotControl().getRemote().respond(messagePackage, new MessagePackage(MessagePackage.Type.DIRECT, messagePackage.getSourceKey()).setBody(result));
        }
    }
}
