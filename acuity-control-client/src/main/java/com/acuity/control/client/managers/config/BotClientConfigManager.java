package com.acuity.control.client.managers.config;

import com.acuity.control.client.BotControl;
import com.acuity.control.client.managers.scripts.ScriptManager;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Created by Zachary Herridge on 9/27/2017.
 */
public class BotClientConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(BotClientConfigManager.class);
    public static final Object LOCK = new Object();

    private BotControl botControl;
    private BotClientConfig currentConfig = new BotClientConfig();

    public BotClientConfigManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void confirmState(){
        botControl.getRemote().confirmState();
    }

    public BotClientConfig getCurrentConfig() {
        return currentConfig;
    }

    public void setSelector(ScriptSelector selector) {
        synchronized (ScriptManager.LOCK){
            logger.info("Setting selector. old={}, new={}", currentConfig.getScriptSelector(), selector);
            currentConfig.setScriptSelector(selector);
        }
    }
}
