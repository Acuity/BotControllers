package com.acuity.control.client.managers.scripts;

import com.acuity.common.util.Pair;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptEvaluator;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ScriptManager {

    private static final Logger logger = LoggerFactory.getLogger(ScriptManager.class);

    public static final Object LOCK = new Object();

    private BotControl botControl;


    private String currentNodeUID;
    private String lastSelectorNodeUID;
    private Map<String, ScriptInstance> scriptInstances = new ConcurrentHashMap<>();

    public ScriptManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void loop() {
        BotClientConfig botClientConfig = botControl.getBotClientConfig();
        if (botClientConfig == null) {
            logger.debug("Null botClientConfig.");
            return;
        }

        String currentNodeUID = this.currentNodeUID;
        ScriptInstance scriptInstance = currentNodeUID != null ? scriptInstances.get(this.currentNodeUID) : null;
        logger.debug("Current execution. {}, {}", currentNodeUID, scriptInstance);

        ScriptNode taskNode = botClientConfig.getTaskNode();
        if (taskNode != null && !taskNode.getUID().equals(currentNodeUID)){
            setCurrentNode(taskNode, 2);
            return;
        }
        else {
            if (botClientConfig.getScriptSelector() != null && botClientConfig.getScriptSelector().getContinuousNodeList() != null){
                for (ScriptNode scriptNode : botClientConfig.getScriptSelector().getContinuousNodeList()) {
                    if (scriptNode.getComplete().orElse(false)) continue;

                    List<ScriptEvaluator> startEvaluators = scriptNode.getEvaluatorGroup().getStartEvaluators();
                    if (startEvaluators != null && ScriptConditionEvaluator.evaluate(botControl, startEvaluators)){
                        setCurrentNode(taskNode, 1);
                        return;
                    }
                }
            }
        }

        if (scriptInstance != null){
            evaluate(scriptInstance);
        }
        else {
            selectNextBaseScript(lastSelectorNodeUID);
        }
    }

    private void selectNextBaseScript(String lastScriptNodeUID){
        BotClientConfig botClientConfig = botControl.getBotClientConfig();
        if (botClientConfig == null) {
            logger.debug("selectNextBaseScript - null botClientConfig.");
            return;
        }

        synchronized (LOCK){
            ScriptSelector scriptSelector = botClientConfig.getScriptSelector();
            if (scriptSelector != null && scriptSelector.getBaseNodeList() != null && scriptSelector.getBaseNodeList().size() > 0){
                logger.debug("Selecting next script. {}", lastScriptNodeUID);

                List<ScriptNode> nodeList = botClientConfig.getScriptSelector().getBaseNodeList();

                int index = -1;
                for (int i = 0; i < nodeList.size(); i++) {
                    ScriptNode scriptNode = nodeList.get(i);
                    if (Objects.equals(scriptNode.getUID(), lastScriptNodeUID)) {
                        index = i;
                        break;
                    }
                }

                logger.debug("Initial index. {}/{}", index, nodeList.size() - 1);
                index++;
                if (index >= nodeList.size()) index = 0;

                ScriptNode scriptNode = nodeList.get(index);
                logger.debug("Next script node. {} @ {}/{}", scriptNode, index, nodeList.size() - 1);

                List<ScriptEvaluator> startEvaluators = scriptNode.getEvaluatorGroup().getStartEvaluators();
                if (startEvaluators == null || ScriptConditionEvaluator.evaluate(botControl, startEvaluators)){
                    setCurrentNode(scriptNode, 0);
                }
                else {
                    logger.info("Failed start evaluation.");
                    selectNextBaseScript(scriptNode.getUID());
                }
            }
            else {
                if (botControl.getRsAccountManager().getRsAccount() != null){
                    botControl.getRsAccountManager().clearRSAccount();
                }
            }
        }
    }

    private void setCurrentNode(ScriptNode scriptNode, int type) {
        synchronized (LOCK){
            if (scriptNode == null){
                currentNodeUID = null;
                return;
            }

            scriptInstances.put(scriptNode.getUID(), new ScriptInstance(scriptNode)
                    .setIncremental(type == 0)
                    .setContinuous(type == 1)
                    .setTask(type == 2)
            );
            currentNodeUID = scriptNode.getUID();
        }
    }

    private boolean evaluate(ScriptInstance scriptInstance){
        List<ScriptEvaluator> runEvaluators = scriptInstance.getScriptNode().getEvaluatorGroup().getRunEvaluators();
        if (runEvaluators != null && !ScriptConditionEvaluator.evaluate(botControl, runEvaluators)){
            onScriptEnded(scriptInstance);
            return false;
        }

        List<ScriptEvaluator> stopEvaluators = scriptInstance.getScriptNode().getEvaluatorGroup().getStopEvaluators();
        if (stopEvaluators != null && ScriptConditionEvaluator.evaluate(botControl, stopEvaluators)){
            onScriptEnded(scriptInstance);
            return false;
        }

        return true;
    }

    public void onBotClientConfigUpdate(BotClientConfig botClientConfig) {
        synchronized (LOCK) {
            logger.debug("Received Updated Client Config - {}.", botClientConfig.hashCode());
            cleanUpScriptInstances();
        }
    }

    private void cleanUpScriptInstances() {
        synchronized (LOCK) {
            logger.debug("Cleaning Up Script Instances.");
        }
    }

    private void destroyInstance(ScriptInstance scriptInstance) {
        try {
            botControl.destroyInstanceOfScript(scriptInstance.getInstance());
        } catch (Throwable e) {
            logger.error("Error during onExit.", e);
        }
        scriptInstances.remove(scriptInstance.getScriptNode().getScriptID());
    }

    public Optional<ScriptNode> getExecutionNode(){
        return getExecutionInstance().map(ScriptInstance::getScriptNode);
    }

    public Optional<ScriptInstance> getExecutionInstance() {
        String currentNodeUID = this.currentNodeUID;
        if (currentNodeUID == null) return Optional.empty();
        return Optional.ofNullable(scriptInstances.get(currentNodeUID));
    }

    public void onScriptEnded(ScriptInstance closedInstance) {
        synchronized (LOCK){
            if (closedInstance == null) return;

            logger.debug("ScriptInstance ended. {}", closedInstance);

            BotClientConfig botClientConfig = botControl.getBotClientConfig();
            ScriptNode scriptNode = closedInstance.getScriptNode();

            if (closedInstance.isTask()){
                ScriptNode taskNode = botClientConfig.getTaskNode();
                if (taskNode != null && taskNode.getUID().equals(scriptNode.getUID())){
                    botClientConfig.setTaskNode(null);
                    boolean updated = botControl.updateClientConfig(botClientConfig, true);
                    logger.debug("ScriptNode was task. removed={}, updated={}", updated);
                }
                destroyInstance(closedInstance);
            }
            else {
                if ((boolean) scriptNode.getSettings().getOrDefault("completeOnStop", false)){
                    logger.debug("ScriptNode was completeOnStop.");
                    scriptNode.setComplete(true);
                    botControl.updateClientConfig(botClientConfig, true);
                }

                if ((boolean) scriptNode.getSettings().getOrDefault("destroyInstanceOnStop", true)){
                    logger.debug("ScriptNode was destroyInstanceOnStop.");
                    destroyInstance(closedInstance);
                }

                if (closedInstance.isIncremental()) lastSelectorNodeUID = scriptNode.getUID();
            }

            if (closedInstance.getScriptNode().getUID().equals(currentNodeUID)) currentNodeUID = null;
        }
    }
}
