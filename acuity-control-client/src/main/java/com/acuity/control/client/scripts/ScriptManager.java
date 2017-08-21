package com.acuity.control.client.scripts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.BotControlEvent;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.scripts.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ScriptManager {

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
        }
        if (!isCurrentScriptRunConfig(botClientConfig.getScriptRunConfig().orElse(null))){
            this.currentRunConfig = botClientConfig.getScriptRunConfig().orElse(null);
            controller.getEventBus().post(new BotControlEvent.ScriptUpdated(this.currentRunConfig));
        }
    }

    public void onScriptEnded() {
        if (lastPair != null) {
            scriptQueue.getConditionalScriptMap().remove(lastPair);
            lastPair = null;
            controller.updateScriptQueue(scriptQueue).waitForResponse(15, TimeUnit.SECONDS);
        }
    }
}
