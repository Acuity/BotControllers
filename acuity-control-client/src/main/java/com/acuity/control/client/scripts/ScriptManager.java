package com.acuity.control.client.scripts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ScriptManager {

    private final Object lock = new Object();
    private Map<ScriptExecutionConfig, Object> scriptInstances = new HashMap<>();
    private ScriptQueue scriptQueue = new ScriptQueue();
    private Pair<ScriptExecutionConfig, Object>  currentScriptExecution;
    private BotControl controller;

    public ScriptManager(BotControl botControl) {
        this.controller = botControl;
    }

    public void onLoop(){
        if (currentScriptExecution != null) currentScriptExecution.getKey().setLastAccount(controller.getRsAccountManager().getRsAccount());

        if (scriptQueue.getConditionalScriptMap().size() == 0){
            if (currentScriptExecution != null){
                currentScriptExecution = null;
                controller.updateCurrentScriptRunConfig(null);
            }
        }
        else {
            for (ScriptExecutionConfig executionConfig : scriptQueue.getConditionalScriptMap()) {
                if (ScriptConditionEvaluator.evaluate(executionConfig.getScriptRunCondition())){
                    if (!isCurrentScriptExecutionConfig(executionConfig)){
                        Object scriptInstance = getScriptInstanceOf(executionConfig);
                        if (scriptInstance != null){
                            handleAccountTransition(executionConfig);
                            currentScriptExecution = new Pair<>(executionConfig, scriptInstance);
                            controller.updateCurrentScriptRunConfig(executionConfig.getScriptRunConfig());
                        }
                    }
                    return;
                }
            }
        }
    }

    private void handleAccountTransition(ScriptExecutionConfig executionConfig){
        RSAccount currentAccount = controller.getRsAccountManager().getRsAccount();
        if (executionConfig.getLastAccount() != null && (currentAccount == null || !executionConfig.getLastAccount().getID().equals(currentAccount.getID()))){
            controller.requestAccountAssignment(executionConfig.getLastAccount(), true);
            return;
        }
        if (currentAccount != null && executionConfig.getScriptRunConfig().getPullAccountsFromTagID() != null && !currentAccount.getTagIDs().contains(executionConfig.getScriptRunConfig().getPullAccountsFromTagID())){
            controller.requestAccountAssignment(null, true);
        }
    }

    public boolean queueStart(ScriptExecutionConfig executionConfig) {
        scriptQueue.getConditionalScriptMap().add(0, executionConfig);
        return controller.updateScriptQueue(scriptQueue)
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(false);
    }

    private boolean isCurrentScriptExecutionConfig(ScriptExecutionConfig config){
        ScriptExecutionConfig currentExecutionConfig = currentScriptExecution != null ? currentScriptExecution.getKey() : null;
        Integer currentConfigHashCode = currentExecutionConfig != null ? currentExecutionConfig.hashCode() : null;
        Integer otherConfigHashCode = config != null ? config.hashCode() : null;
        return Objects.equals(currentConfigHashCode, otherConfigHashCode);
    }

    public void onBotClientConfigUpdate(BotClientConfig botClientConfig){
        if (scriptQueue.hashCode() != botClientConfig.getScriptQueue().hashCode()) {
            this.scriptQueue = botClientConfig.getScriptQueue();
            synchronized (lock){
                List<ScriptExecutionConfig> conditionalScriptMap = scriptQueue.getConditionalScriptMap();
                List<ScriptExecutionConfig> toRemove = scriptInstances.keySet().stream().filter(executionConfig -> !conditionalScriptMap.contains(executionConfig)).collect(Collectors.toList());
                toRemove.forEach(s -> scriptInstances.remove(s));
            }
        }
    }

    private Object getScriptInstanceOf(ScriptExecutionConfig executionConfig){
        if (executionConfig != null) {
            if (!scriptInstances.containsKey(executionConfig)) {
                synchronized (lock){
                    Object instanceOfScript = controller.createInstanceOfScript(executionConfig.getScriptRunConfig());
                    if (instanceOfScript != null) {
                        scriptInstances.put(executionConfig, instanceOfScript);
                        return instanceOfScript;
                    }
                }
            }
            return scriptInstances.get(executionConfig);
        }
        return null;
    }

    public Pair<ScriptExecutionConfig, Object> getScriptInstance(){
        return currentScriptExecution;
    }

    public void onScriptEnded(Pair<ScriptExecutionConfig, Object> closedScript) {
        synchronized (lock){
            if (closedScript.getKey().isRemoveOnEnd()){
                scriptQueue.getConditionalScriptMap().removeIf(pair -> pair.getScriptRunConfig().getRunConfigID().equals(closedScript.getKey().getScriptRunConfig().getRunConfigID()));
                controller.updateScriptQueue(scriptQueue).waitForResponse(15, TimeUnit.SECONDS);
                scriptInstances.remove(closedScript.getKey());
                controller.destroyInstanceOfScript(closedScript.getValue());
            }
        }
    }
}
