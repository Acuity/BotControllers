package com.acuity.control.client.managers;

import com.acuity.control.client.BotControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zachary Herridge on 10/5/2017.
 */
public class ExecutorManager {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorManager.class);

    private BotControl botControl;

    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    private ScheduledExecutorService scriptExecutorService = Executors.newSingleThreadScheduledExecutor();

    public ExecutorManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (botControl.getConnection().isConnected()) {
                    botControl.getClientInterface().sendClientState();
                }
            } catch (Throwable e) {
                logger.error("Error during sending client state.", e);
            }

        }, 3, 5, TimeUnit.SECONDS);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (botControl.getConnection().isConnected()) {
                    botControl.getConnection().sendScreenCapture(2);
                }
            } catch (Throwable e) {
                logger.error("Error during screen capture.", e);
            }
        }, 3, 10, TimeUnit.SECONDS);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (botControl.getConnection().isConnected()){
                    botControl.getClientConfigManager().confirmState();
                }
            }
            catch (Throwable e){
                logger.error("Error during ConfigManager confirmState.", e);
            }
        }, 30, 45, TimeUnit.SECONDS);

        scriptExecutorService.scheduleAtFixedRate(() -> {
            try {
                botControl.getScriptManager().loop();
            }
            catch (Throwable e){
                logger.error("Error during script manager confirmState.", e);
            }
        }, 3, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduledExecutorService.shutdownNow();
        scriptExecutorService.shutdownNow();
    }
}
