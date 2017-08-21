package com.acuity.control.client.scripts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.BotControlEvent;
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
    private Pair<ScriptRunCondition, ScriptRunConfig> lastPair;


    public ScriptManager(BotControl botControl) {
        this.controller = botControl;
    }

    public void onLoop(){
        for (Pair<ScriptRunCondition, ScriptRunConfig> pair : scriptQueue.getConditionalScriptMap()) {
            if (ScriptConditionEvaluator.evaluate(pair.getKey().getConditions())){
                if (!isCurrentScriptRunConfig(pair.getValue())){
                    lastPair = pair;
                    controller.requestScript(pair.getValue());
                }
                return;
            }
        }
    }

    public boolean isCurrentScriptRunConfig(ScriptRunConfig config){
        Integer currentConfigHashCode = currentRunConfig != null ? currentRunConfig.hashCode() : null;
        Integer otherConfigHashCode = config != null ? config.hashCode() : null;
        return Objects.equals(currentConfigHashCode, otherConfigHashCode);
    }

    public void onBotClientConfigUpdate(BotClientConfig botClientConfig){
        if (scriptQueue.hashCode() != botClientConfig.getScriptQueue().hashCode()) {
            this.scriptQueue = botClientConfig.getScriptQueue();
            synchronized (lock){
                List<String> ids = scriptQueue.getConditionalScriptMap().stream().map(pair -> pair.getValue().getRunConfigID()).collect(Collectors.toList());
                List<String> toRemove = scriptInstances.values().stream().filter(pair -> !ids.contains(pair.getKey().getRunConfigID())).map(pair -> pair.getKey().getRunConfigID()).collect(Collectors.toList());
                toRemove.forEach(s -> scriptInstances.remove(s));
            }
        }
        if (!isCurrentScriptRunConfig(botClientConfig.getScriptRunConfig().orElse(null))){
            this.currentRunConfig = botClientConfig.getScriptRunConfig().orElse(null);
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

    public void onScriptEnded(Pair<ScriptRunConfig, Object> closeScript) {
        synchronized (lock){
            scriptQueue.getConditionalScriptMap().removeIf(pair -> pair.getValue().getRunConfigID().equals(closeScript.getKey().getRunConfigID()));
            controller.updateScriptQueue(scriptQueue).waitForResponse(15, TimeUnit.SECONDS);
            scriptInstances.remove(closeScript.getKey().getRunConfigID());
            controller.destroyInstanceOfScript(closeScript.getValue());
        }
    }
}
