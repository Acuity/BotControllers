package com.acuity.control.client.managers.scripts.instance;

import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.google.common.base.MoreObjects;

/**
 * Created by Zach on 10/1/2017.
 */
public class ScriptInstance {

    private boolean task;
    private boolean continuous;
    private boolean incremental;

    private ScriptNode scriptNode;
    private Object instance;

    public ScriptInstance(ScriptNode scriptNode) {
        this.scriptNode = scriptNode;
    }

    public ScriptInstance setContinuous(boolean continuous) {
        this.continuous = continuous;
        return this;
    }

    public ScriptInstance setIncremental(boolean incremental) {
        this.incremental = incremental;
        return this;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public boolean isTask() {
        return task;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public ScriptInstance setTask(boolean task) {
        this.task = task;
        return this;
    }

    public ScriptNode getScriptNode() {
        return scriptNode;
    }

    public ScriptInstance setScriptNode(ScriptNode scriptNode) {
        this.scriptNode = scriptNode;
        return this;
    }

    public Object getInstance() {
        return instance;
    }

    public ScriptInstance setInstance(Object instance) {
        this.instance = instance;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("task", task)
                .add("continuous", continuous)
                .add("incremental", incremental)
                .add("scriptNode", scriptNode)
                .add("instance", instance)
                .toString();
    }
}
