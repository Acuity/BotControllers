package com.acuity.control.client.managers.scripts;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptEvaluator;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ScriptManager {

    public static final Object LOCK = new Object();

    private static final Logger logger = LoggerFactory.getLogger(ScriptManager.class);
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
            logger.debug("BotClientConfig not found, skipping script evaluations.");
            return;
        }

        synchronized (LOCK){
            if (botControl.executeLoginHandler()) {
                logger.warn("Login handler executing, skipping instance evaluation.");
                return;
            }

            String currentNodeUID = this.currentNodeUID;
            ScriptInstance scriptInstance = currentNodeUID != null ? scriptInstances.get(this.currentNodeUID) : null;
            logger.trace("Current execution. {}, {}", scriptInstance, currentNodeUID);

            ScriptNode taskNode = botControl.getTaskManager().getCurrentTask();
            if (taskNode != null && !taskNode.getUID().equals(currentNodeUID)) {
                logger.debug("Task node found, setting as current node. {}", taskNode);
                setCurrentNode(taskNode, 2);
                return;
            } else {
                if (scriptInstance == null || scriptInstance.isIncremental()) {
                    if (botClientConfig.getScriptSelector() != null && botClientConfig.getScriptSelector().getContinuousNodeList() != null) {
                        for (ScriptNode continuousNode : botClientConfig.getScriptSelector().getContinuousNodeList()) {
                            if (continuousNode.getComplete().orElse(false)) continue;

                            List<ScriptEvaluator> startEvaluators = continuousNode.getEvaluatorGroup().getStartEvaluators();
                            if (startEvaluators != null && ScriptConditionEvaluator.evaluate(botControl, startEvaluators)) {
                                logger.debug("Continuous node found, setting as current node. {}", continuousNode);
                                setCurrentNode(continuousNode, 1);
                                return;
                            }
                        }
                    }
                }
            }

            if (currentNodeUID != null) {
                evaluate(scriptInstance);
            } else {
                selectNextBaseScript(lastSelectorNodeUID, 0);
            }
        }
    }

    private boolean selectNextBaseScript(String lastScriptNodeUID, int attempt) {
        BotClientConfig botClientConfig = botControl.getBotClientConfig();
        if (botClientConfig == null) {
            logger.debug("selectNextBaseScript - null botClientConfig.");
            return false;
        }

        ScriptSelector scriptSelector = botClientConfig.getScriptSelector();
        if (scriptSelector != null && scriptSelector.getBaseNodeList() != null && scriptSelector.getBaseNodeList().size() > 0) {
            logger.debug("Selecting next script. {}", lastScriptNodeUID);

            List<ScriptNode> nodeList = botClientConfig.getScriptSelector().getBaseNodeList();

            if (attempt > nodeList.size()) return false;

            int index = -1;
            for (int i = 0; i < nodeList.size(); i++) {
                ScriptNode scriptNode = nodeList.get(i);
                if (Objects.equals(scriptNode.getUID(), lastScriptNodeUID)) {
                    index = i;
                    break;
                }
            }

            logger.trace("Initial index. {}/{}", index, nodeList.size() - 1);
            index++;
            if (index >= nodeList.size()) index = 0;

            ScriptNode scriptNode = nodeList.get(index);
            logger.trace("Next script node. {} @ {}/{}", scriptNode, index, nodeList.size() - 1);

            List<ScriptEvaluator> startEvaluators = scriptNode.getEvaluatorGroup().getStartEvaluators();
            if (startEvaluators == null || ScriptConditionEvaluator.evaluate(botControl, startEvaluators)) {
                setCurrentNode(scriptNode, 0);
                return true;
            } else {
                logger.info("Failed start evaluation.");
                return selectNextBaseScript(scriptNode.getUID(), attempt + 1);
            }
        }
        return false;
    }

    private void setCurrentNode(ScriptNode scriptNode, int type) {
        if (scriptNode == null) {
            currentNodeUID = null;
            return;
        }

        ScriptInstance scriptInstance = new ScriptInstance(scriptNode)
                .setIncremental(type == 0)
                .setContinuous(type == 1)
                .setTask(type == 2);

        scriptInstances.put(scriptNode.getUID(), scriptInstance);
        currentNodeUID = scriptNode.getUID();

        logger.info("Set current script node/instance. {}", scriptInstance);
    }

    private boolean evaluate(ScriptInstance scriptInstance) {
        List<ScriptEvaluator> runEvaluators = scriptInstance.getScriptNode().getEvaluatorGroup().getRunEvaluators();
        if (runEvaluators != null && !ScriptConditionEvaluator.evaluate(botControl, runEvaluators)) {
            onScriptEnded(scriptInstance);
            return false;
        }

        List<ScriptEvaluator> stopEvaluators = scriptInstance.getScriptNode().getEvaluatorGroup().getStopEvaluators();
        if (stopEvaluators != null && ScriptConditionEvaluator.evaluate(botControl, stopEvaluators)) {
            onScriptEnded(scriptInstance);
            return false;
        }

        return true;
    }

    private void destroyInstance(ScriptInstance scriptInstance) {
        if (scriptInstance == null) return;

        if (scriptInstance.getInstance() != null) {
            try {
                botControl.destroyInstanceOfScript(scriptInstance.getInstance());
            } catch (Throwable e) {
                logger.error("Error during onExit.", e);
            }
        }

        ScriptInstance remove = scriptInstances.remove(scriptInstance.getScriptNode().getUID());
        logger.debug("Destroyed ScriptInstance, {}/{}.", remove != null, scriptInstance);
    }

    public Optional<ScriptNode> getExecutionNode() {
        return getExecutionInstance().map(ScriptInstance::getScriptNode);
    }

    public Optional<ScriptInstance> getExecutionInstance() {
        String currentNodeUID = this.currentNodeUID;
        if (currentNodeUID == null) return Optional.empty();
        return Optional.ofNullable(scriptInstances.get(currentNodeUID));
    }

    private void transitionAccount(ScriptInstance closedInstance) {
        if (closedInstance.isTask()) {
            botControl.getRsAccountManager().clearRSAccount();
        }
    }

    public void onScriptEnded(ScriptInstance closedInstance) {
        if (closedInstance == null) return;

        synchronized (LOCK){
            logger.debug("ScriptInstance ended. {}", closedInstance);

            ScriptNode scriptNode = closedInstance.getScriptNode();
            if (closedInstance.isTask()) {
                ScriptNode taskNode = botControl.getTaskManager().getCurrentTask();
                if (taskNode != null && taskNode.getUID().equals(scriptNode.getUID())) {
                    botControl.getTaskManager().setCurrentTask(null);
                    logger.debug("ScriptNode was task.");
                }
                destroyInstance(closedInstance);
            } else {
                if ((boolean) scriptNode.getSettings().getOrDefault("destroyInstanceOnStop", true)) {
                    logger.debug("ScriptNode was destroyInstanceOnStop.");
                    destroyInstance(closedInstance);
                }

                if (closedInstance.isIncremental()) lastSelectorNodeUID = scriptNode.getUID();
            }

            if (closedInstance.getScriptNode().getUID().equals(currentNodeUID)) {
                currentNodeUID = null;
                transitionAccount(closedInstance);
            }
        }
    }
}
