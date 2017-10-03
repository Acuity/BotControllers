package com.acuity.control.client.managers.config;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
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
        synchronized (LOCK){
            botControl.confirmState();
        }
    }

    public void setCurrentConfig(BotClientConfig config) {
        synchronized (LOCK){
            logger.debug("BotClientConfig updated. new={}, old={}", config, currentConfig);
            this.currentConfig = config;
            botControl.getScriptManager().onBotClientConfigUpdate(currentConfig);
            botControl.getBreakManager().onBotClientConfigUpdate(currentConfig);
            botControl.getProxyManager().onBotClientConfigUpdate(currentConfig);
        }
    }

    public BotClientConfig getCurrentConfig() {
        return currentConfig;
    }
}
