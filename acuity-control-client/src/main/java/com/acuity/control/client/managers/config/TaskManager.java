package com.acuity.control.client.managers.config;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Zachary Herridge on 10/4/2017.
 */
public class TaskManager {

    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    private BotControl botControl;
    private volatile ScriptNode currentTask;

    public TaskManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public ScriptNode getCurrentTask() {
        return currentTask;
    }

    public TaskManager setCurrentTask(ScriptNode currentTask) {
        logger.info("Setting current task. old={}, new={}", this.currentTask, currentTask);
        this.currentTask = currentTask;
        return this;
    }
}
