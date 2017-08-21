package com.acuity.control.client;

import com.acuity.db.domain.vertex.impl.scripts.ScriptRunConfig;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class BotControlEvent {

    public static class ProxyUpdated{

    }

    public static class ScriptUpdated{

        private ScriptRunConfig currentRunConfig;

        public ScriptUpdated(ScriptRunConfig currentRunConfig) {
            this.currentRunConfig = currentRunConfig;
        }

        public ScriptRunConfig getCurrentRunConfig() {
            return currentRunConfig;
        }
    }
}
