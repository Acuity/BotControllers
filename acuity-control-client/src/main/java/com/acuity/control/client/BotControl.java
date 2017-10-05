package com.acuity.control.client;

import com.acuity.common.world_data_parser.WorldDataResult;
import com.acuity.control.client.managers.ClientManager;
import com.acuity.control.client.managers.ExecutorManager;
import com.acuity.control.client.managers.RemoteManager;
import com.acuity.control.client.managers.accounts.RSAccountManager;
import com.acuity.control.client.managers.breaks.BreakManager;
import com.acuity.control.client.managers.config.BotClientConfigManager;
import com.acuity.control.client.managers.config.TaskManager;
import com.acuity.control.client.managers.proxies.ProxyManager;
import com.acuity.control.client.managers.scripts.ScriptManager;
import com.acuity.control.client.network.BotControlConnection;
import com.acuity.control.client.util.RemotePrintStream;
import com.acuity.control.client.network.response.MessageResponse;
import com.acuity.control.client.managers.world.WorldManager;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClient;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.message_package.data.RemoteScriptTask;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.Script;
import com.acuity.db.domain.vertex.impl.scripts.ScriptVersion;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.acuity.db.domain.vertex.impl.tag.Tag;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zach on 8/20/2017.
 */
public class BotControl implements SubscriberExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BotControl.class);

    
    private EventBus eventBus = new EventBus(this);

    private BotClientConfigManager clientConfigManager = new BotClientConfigManager(this);
    private ScriptManager scriptManager = new ScriptManager(this);
    private BreakManager breakManager = new BreakManager(this);
    private RSAccountManager rsAccountManager = new RSAccountManager(this);
    private ProxyManager proxyManager = new ProxyManager(this);
    private WorldManager worldManager = new WorldManager(this);
    private TaskManager taskManager = new TaskManager(this);
    private RemoteManager remoteManager = new RemoteManager(this);
    private ExecutorManager executorManager = new ExecutorManager(this);
    private ClientManager clientManager = null;

    private BotControlConnection connection;

    public BotControl(String host, ClientType clientType, ClientManager clientManager) {
        this.clientManager = clientManager;
        this.clientManager.setBotControl(this);

        this.connection = new BotControlConnection(this, host, clientType);

        executorManager.start();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public RSAccountManager getRsAccountManager() {
        return rsAccountManager;
    }

    public BreakManager getBreakManager() {
        return breakManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public BotClientConfigManager getClientConfigManager() {
        return clientConfigManager;
    }

    public RemoteManager getRemote() {
        return remoteManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public ClientManager getClientInterface() {
        return clientManager;
    }

    public BotControlConnection getConnection() {
        return connection;
    }

    @Override
    public void handleException(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext) {
        throwable.printStackTrace();
    }

    public void stop() {
        executorManager.stop();
        connection.stop();
    }

    public BotClientConfig getBotClientConfig() {
        return clientConfigManager.getCurrentConfig();
    }
}
