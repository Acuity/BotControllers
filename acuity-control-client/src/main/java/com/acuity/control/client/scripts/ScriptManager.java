package com.acuity.control.client.scripts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ScriptManager {

    private static final Logger logger = LoggerFactory.getLogger(ScriptManager.class);

    private final Object lock = new Object();

    private BotControl botControl;

    private Pair<ScriptNode, Object> currentScriptPair;
    private Map<String, Object> scriptInstances = new ConcurrentHashMap<>();

    public ScriptManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void onLoop() {
        BotClientConfig botClientConfig = botControl.getBotClientConfig();
        if (botClientConfig == null) {
            logger.debug("onLoop - null botClientConfig.");
            return;
        }

        Pair<ScriptNode, Object> currentScriptPair = this.currentScriptPair;
        ScriptNode currentScriptNode = currentScriptPair != null ? currentScriptPair.getKey() : null;
        boolean currentScriptValid = currentScriptNode != null && botClientConfig.getScriptSelector().getNodeList().contains(currentScriptNode);

        if (currentScriptValid){
            if (!ScriptConditionEvaluator.evaluate(botControl, currentScriptNode.getEvaluatorGroup().getRunEvaluators())){
                onScriptEnded(currentScriptPair);
            }
            else if (ScriptConditionEvaluator.evaluate(botControl, currentScriptNode.getEvaluatorGroup().getStopEvaluators())){
                onScriptEnded(currentScriptPair);
            }
        }
        else {
            for (ScriptNode scriptNode : botClientConfig.getScriptSelector().getNodeList()) {
                if (scriptNode.isComplete()) continue;

                if (ScriptConditionEvaluator.evaluate(botControl, scriptNode.getEvaluatorGroup().getStartEvaluators())){
                    this.currentScriptPair = new Pair<>(scriptNode, getScriptInstanceOf(scriptNode));
                    return;
                }
            }
        }
    }

    private void handleAccountTransition(ScriptExecutionConfig executionConfig) {
        if (executionConfig == null) {
            logger.debug("Handling Account Transition - no next script.");
            botControl.getRsAccountManager().clearRSAccount();
            botControl.requestAccountAssignment(null, true);
            return;
        }

        logger.debug("Handling Account Transition - {}.", executionConfig.getUID());
        RSAccount currentAccount = botControl.getRsAccountManager().getRsAccount();
        if (executionConfig.getLastAccount() != null && (currentAccount == null || !executionConfig.getLastAccount().getID().equals(currentAccount.getID()))) {
            if (!botControl.requestAccountAssignment(executionConfig.getLastAccount(), false)) {
                botControl.getRsAccountManager().clearRSAccount();
                botControl.requestAccountAssignment(null, true);
            }
        }
        if (currentAccount != null && executionConfig.getScriptStartupConfig().getPullAccountsFromTagID() != null && !currentAccount.getTagIDs().contains(executionConfig.getScriptStartupConfig().getPullAccountsFromTagID())) {
            botControl.getRsAccountManager().clearRSAccount();
            botControl.requestAccountAssignment(null, true);
        }
    }

    public boolean queueTask(int index, ScriptExecutionConfig executionConfig) {
        logger.debug("Queueing Task - {}, {}.", executionConfig.getUID(), index);
        botClientConfig.getTaskRoutine().getConditionalScriptMap().add(index, executionConfig);
        return botControl.updateTaskRoutine(botClientConfig.getTaskRoutine())
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(false);
    }

    public void onBotClientConfigUpdate(BotClientConfig botClientConfig) {
        synchronized (lock) {
            logger.debug("Received Updated Client Config - {}.", botClientConfig.hashCode());
            boolean update = this.botClientConfig == null || (this.botClientConfig.getScriptRoutine().hashCode() != botClientConfig.getScriptRoutine().hashCode() || this.botClientConfig.getTaskRoutine().hashCode() != botClientConfig.getTaskRoutine().hashCode());
            this.botClientConfig = botClientConfig;
            if (update) {
                cleanUpScriptInstances();
            }
        }
    }

    private void cleanUpScriptInstances() {
        synchronized (lock) {
            logger.debug("Cleaning Up Script Instances.");
        }
    }

    private void destroyInstance(Object instance) {
        try {
            botControl.destroyInstanceOfScript(instance);
        } catch (Throwable e) {
            logger.error("Error during onExit.", e);
        }
    }


    private static final Object lock2 = new Object();
    private Object getScriptInstanceOf(ScriptNode scriptNode) {
        synchronized (lock2) {
            if (scriptNode != null) {
                if (!scriptInstances.containsKey(scriptNode.getUID())) {
                    logger.debug("Creating Script Instance - {}.", scriptNode.getUID());
                    Object instanceOfScript = botControl.createInstanceOfScript(scriptNode);
                    logger.debug("Creation of {} complete. {}", instanceOfScript);
                    if (instanceOfScript != null) {
                        scriptInstances.put(scriptNode.getUID(), instanceOfScript);
                        return instanceOfScript;
                    }

                }
                return scriptInstances.get(scriptNode.getUID());
            }
        }
        return null;
    }

    public Optional<Pair<ScriptNode, Object>> getScriptInstance() {
        return Optional.ofNullable(currentScriptPair);
    }

    public void onScriptEnded(Pair<ScriptNode, Object> closedScript) {
        if (closedScript == null) return;

        synchronized (lock) {
            logger.debug("Script Ended - {}.", closedScript.getKey().getUID());
            boolean wasTask = botClientConfig.getTaskRoutine().getConditionalScriptMap().removeIf(executionConfig -> executionConfig.getUID().equals(closedScript.getKey().getUID()));
            if (wasTask) {
                logger.debug("Removing Task - {}.", closedScript.getKey().getUID());
                botControl.updateTaskRoutine(botClientConfig.getTaskRoutine()).waitForResponse(15, TimeUnit.SECONDS).ifPresent(messagePackage -> {
                    scriptInstances.remove(closedScript.getKey());
                    destroyInstance(closedScript.getValue());
                });
            } else {
                if (closedScript.getKey().isRemoveOnEnd()) {
                    logger.debug("Removing Script - {}.", closedScript.getKey().getUID());
                    botClientConfig.getScriptRoutine().getConditionalScriptMap().removeIf(executionConfig -> executionConfig.getUID().equals(closedScript.getKey().getUID()));
                    botControl.updateScriptRoutine(botClientConfig.getScriptRoutine()).waitForResponse(15, TimeUnit.SECONDS);
                } else {
                    logger.debug("Moving Script to end - {}.", closedScript.getKey().getUID());
                    List<ScriptExecutionConfig> conditionalScriptMap = botClientConfig.getScriptRoutine().getConditionalScriptMap();
                    conditionalScriptMap.add(conditionalScriptMap.size() - 1, conditionalScriptMap.remove(0));
                    botControl.updateScriptRoutine(botClientConfig.getScriptRoutine()).waitForResponse(15, TimeUnit.SECONDS);
                }

                if (closedScript.getKey().isDestroyInstanceOnEnd()){
                    logger.debug("Destroying instance of script. {}", closedScript.getKey().getUID());
                    scriptInstances.remove(closedScript.getKey().getUID());
                    destroyInstance(closedScript.getValue());
                }
            }

            handleAccountTransition(getScriptInstance().map(Pair::getKey).orElse(null));
        }
    }
}
