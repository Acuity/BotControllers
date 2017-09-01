package com.acuity.control.client.scripts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.ScriptExecutionConfig;
import com.acuity.db.domain.vertex.impl.scripts.ScriptRoutine;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ScriptManager {

    private final Object lock = new Object();

    private BotControl botControl;
    private Map<ScriptExecutionConfig, Object> scriptInstances = new HashMap<>();

    private BotClientConfig botClientConfig;

    private Pair<ScriptExecutionConfig, Object> currentScriptExecution;

    public ScriptManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void onLoop() {
        if (botClientConfig == null) return;

        if (botClientConfig.getTaskRoutine().getConditionalScriptMap().size() > 0){
            ScriptExecutionConfig executionConfig = botClientConfig.getTaskRoutine().getConditionalScriptMap().get(0);
            if (!isCurrentScriptExecutionConfig(executionConfig)){
                Object scriptInstanceOf = getScriptInstanceOf(executionConfig);
                if (scriptInstanceOf != null) {
                    currentScriptExecution = new Pair<>(executionConfig, scriptInstanceOf);
                    handleAccountTransition(executionConfig);
                }
                return;
            }
        }

        if (currentScriptExecution != null) {
            boolean endConditionMet = botControl.evaluate(currentScriptExecution.getKey().getEndCondition());
            if (endConditionMet) {
                onScriptEnded(currentScriptExecution);
            }
            else {
                currentScriptExecution.getKey().setLastAccount(botControl.getRsAccountManager().getRsAccount());
                LocalDateTime endTime = currentScriptExecution.getKey().getScriptStartupConfig().getEndTime().orElse(null);
                if (endTime != null && LocalDateTime.now().isAfter(endTime)) {
                    onScriptEnded(currentScriptExecution);
                }
            }
        }
        else {
            ScriptRoutine scriptRoutine = botClientConfig.getScriptRoutine();
            int nextIndex = scriptRoutine.getCurrentIndex() + 1;
            if (nextIndex >= scriptRoutine.getConditionalScriptMap().size()) nextIndex = 0;
            scriptRoutine.setCurrentIndex(nextIndex);
            botControl.updateScriptRoutine(scriptRoutine).waitForResponse(15, TimeUnit.SECONDS);
        }
    }

    private void handleAccountTransition(ScriptExecutionConfig executionConfig) {
        RSAccount currentAccount = botControl.getRsAccountManager().getRsAccount();
        if (executionConfig.getLastAccount() != null && (currentAccount == null || !executionConfig.getLastAccount().getID().equals(currentAccount.getID()))) {
            if (!botControl.requestAccountAssignment(executionConfig.getLastAccount(), false)) {
                botControl.requestAccountAssignment(null, true);
            }
        }
        if (currentAccount != null && executionConfig.getScriptStartupConfig().getPullAccountsFromTagID() != null && !currentAccount.getTagIDs().contains(executionConfig.getScriptStartupConfig().getPullAccountsFromTagID())) {
            botControl.requestAccountAssignment(null, true);
        }
    }

    public boolean queueTask(int index, ScriptExecutionConfig executionConfig) {
        botClientConfig.getTaskRoutine().getConditionalScriptMap().add(index, executionConfig);
        return botControl.updateTaskRoutine(botClientConfig.getTaskRoutine())
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(false);
    }

    private boolean isCurrentScriptExecutionConfig(ScriptExecutionConfig config) {
        ScriptExecutionConfig currentExecutionConfig = currentScriptExecution != null ? currentScriptExecution.getKey() : null;
        Integer currentConfigHashCode = currentExecutionConfig != null ? currentExecutionConfig.hashCode() : null;
        Integer otherConfigHashCode = config != null ? config.hashCode() : null;
        return Objects.equals(currentConfigHashCode, otherConfigHashCode);
    }

    public void onBotClientConfigUpdate(BotClientConfig botClientConfig) {
        boolean update = this.botClientConfig == null || (this.botClientConfig.getScriptRoutine().hashCode() != botClientConfig.getScriptRoutine().hashCode() || this.botClientConfig.getTaskRoutine().hashCode() != botClientConfig.getTaskRoutine().hashCode());
        this.botClientConfig = botClientConfig;
        if (update) cleanUpScriptInstances();
    }

    private void cleanUpScriptInstances(){
        synchronized (lock) {
            List<ScriptExecutionConfig> allScriptExecutions = botClientConfig.getTaskRoutine().getConditionalScriptMap();
            allScriptExecutions.addAll(botClientConfig.getScriptRoutine().getConditionalScriptMap());
            List<ScriptExecutionConfig> toRemove = scriptInstances.keySet().stream().filter(executionConfig -> !allScriptExecutions.contains(executionConfig)).collect(Collectors.toList());
            toRemove.forEach(s -> {
                Object instance = scriptInstances.get(s);
                if (instance != null) botControl.destroyInstanceOfScript(instance);
                scriptInstances.remove(s);
            });
        }
    }

    private Object getScriptInstanceOf(ScriptExecutionConfig executionConfig) {
        if (executionConfig != null) {
            if (!scriptInstances.containsKey(executionConfig)) {
                synchronized (lock) {
                    Object instanceOfScript = botControl.createInstanceOfScript(executionConfig.getScriptStartupConfig());
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

    public Pair<ScriptExecutionConfig, Object> getScriptInstance() {
        return currentScriptExecution;
    }

    public void onScriptEnded(Pair<ScriptExecutionConfig, Object> closedScript) {
        synchronized (lock) {
            boolean wasTask = botClientConfig.getTaskRoutine().getConditionalScriptMap().removeIf(executionConfig -> executionConfig.getUID().equals(closedScript.getKey().getUID()));
            if (wasTask){
                botControl.updateTaskRoutine(botClientConfig.getTaskRoutine()).waitForResponse(15, TimeUnit.SECONDS).ifPresent(messagePackage -> {
                    scriptInstances.remove(closedScript.getKey());
                    botControl.destroyInstanceOfScript(closedScript.getValue());
                    onLoop();
                });
            }
            else if (closedScript.getKey().isRemoveOnEnd()) {
                botClientConfig.getScriptRoutine().getConditionalScriptMap().removeIf(executionConfig -> executionConfig.getUID().equals(closedScript.getKey().getUID()));
                botControl.updateScriptRoutine(botClientConfig.getScriptRoutine()).waitForResponse(15, TimeUnit.SECONDS).ifPresent(messagePackage -> {
                    scriptInstances.remove(closedScript.getKey());
                    botControl.destroyInstanceOfScript(closedScript.getValue());
                    onLoop();
                });
            }
        }
    }
}
