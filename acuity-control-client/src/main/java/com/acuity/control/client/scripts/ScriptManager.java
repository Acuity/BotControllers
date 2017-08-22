package com.acuity.control.client.scripts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
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
    private Map<String, Pair<ScriptRunConfig, Object>> scriptInstances = new HashMap<>();
    private ScriptQueue scriptQueue = new ScriptQueue();
    private ScriptRunConfig currentRunConfig;
    private BotControl controller;

    public ScriptManager(BotControl botControl) {
        this.controller = botControl;
    }

    public void onLoop(){
        if (scriptQueue.getConditionalScriptMap().size() == 0){
            if (currentRunConfig == null){
                currentRunConfig = null;
                controller.updateCurrentScriptRunConfig(null);
            }
        }
        else {
            for (ScriptExecutionConfig pair : scriptQueue.getConditionalScriptMap()) {
                if (ScriptConditionEvaluator.evaluate(pair.getScriptRunCondition())){
                    if (!isCurrentScriptRunConfig(pair.getScriptRunConfig())){
                        currentRunConfig = pair.getScriptRunConfig();
                        controller.updateCurrentScriptRunConfig(pair.getScriptRunConfig());
                    }
                    return;
                }
            }
        }
    }

    private boolean isCurrentScriptRunConfig(ScriptRunConfig config){
        Integer currentConfigHashCode = currentRunConfig != null ? currentRunConfig.hashCode() : null;
        Integer otherConfigHashCode = config != null ? config.hashCode() : null;
        return Objects.equals(currentConfigHashCode, otherConfigHashCode);
    }

    public void onBotClientConfigUpdate(BotClientConfig botClientConfig){
        if (scriptQueue.hashCode() != botClientConfig.getScriptQueue().hashCode()) {
            this.scriptQueue = botClientConfig.getScriptQueue();
            synchronized (lock){
                List<String> ids = scriptQueue.getConditionalScriptMap().stream().map(pair -> pair.getScriptRunConfig().getRunConfigID()).collect(Collectors.toList());
                List<String> toRemove = scriptInstances.values().stream().filter(pair -> !ids.contains(pair.getKey().getRunConfigID())).map(pair -> pair.getKey().getRunConfigID()).collect(Collectors.toList());
                toRemove.forEach(s -> scriptInstances.remove(s));
            }
        }
    }

    public Pair<ScriptRunConfig, Object> getScriptInstance(){
        ScriptRunConfig currentRunConfig = this.currentRunConfig;
        if (currentRunConfig != null) {
            if (!scriptInstances.containsKey(currentRunConfig.getRunConfigID())) {
                synchronized (lock){
                    Object instanceOfScript = controller.createInstanceOfScript(currentRunConfig);
                    if (instanceOfScript != null) {
                        Pair<ScriptRunConfig, Object> pair = new Pair<>(currentRunConfig, instanceOfScript);
                        scriptInstances.put(currentRunConfig.getRunConfigID(), pair);
                        return pair;
                    }
                }
            }
            return scriptInstances.get(currentRunConfig.getRunConfigID());
        }
        return null;
    }

    public void onScriptEnded(Pair<ScriptRunConfig, Object> closedScript) {
        synchronized (lock){
            scriptQueue.getConditionalScriptMap().removeIf(pair -> pair.getScriptRunConfig().getRunConfigID().equals(closedScript.getKey().getRunConfigID()));
            controller.updateScriptQueue(scriptQueue).waitForResponse(15, TimeUnit.SECONDS);
            scriptInstances.remove(closedScript.getKey().getRunConfigID());
            controller.destroyInstanceOfScript(closedScript.getValue());
        }
    }
}
