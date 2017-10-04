package com.acuity.control.client.managers.config;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;

/**
 * Created by Zachary Herridge on 10/4/2017.
 */
public class TaskManager {

    private BotControl botControl;
    private ScriptNode currentTask;

    public TaskManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public ScriptNode getCurrentTask() {
        return currentTask;
    }

    public TaskManager setCurrentTask(ScriptNode currentTask) {
        this.currentTask = currentTask;
        return this;
    }
}
