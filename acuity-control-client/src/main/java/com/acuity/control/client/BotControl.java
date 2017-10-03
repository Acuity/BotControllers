package com.acuity.control.client;

import com.acuity.common.world_data_parser.WorldDataResult;
import com.acuity.control.client.managers.accounts.RSAccountManager;
import com.acuity.control.client.managers.breaks.BreakManager;
import com.acuity.control.client.managers.config.BotClientConfigManager;
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
public abstract class BotControl implements SubscriberExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BotControl.class);

    private EventBus eventBus = new EventBus(this);

    private BotClientConfigManager clientConfigManager = new BotClientConfigManager(this);
    private ScriptManager scriptManager = new ScriptManager(this);
    private BreakManager breakManager = new BreakManager(this);
    private RSAccountManager rsAccountManager = new RSAccountManager(this);
    private ProxyManager proxyManager = new ProxyManager(this);
    private WorldManager worldManager = new WorldManager(this);

    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    private ScheduledExecutorService scriptExecutorService = Executors.newSingleThreadScheduledExecutor();
    private BotControlConnection connection;

    public BotControl(String host, ClientType clientType) {
        this.connection = new BotControlConnection(this, host, clientType);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (connection.isConnected()) {
                    sendClientState();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }, 3, 5, TimeUnit.SECONDS);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (connection.isConnected()) {
                    connection.sendScreenCapture(2);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }, 3, 10, TimeUnit.SECONDS);

        scriptExecutorService.scheduleAtFixedRate(() -> {
            try {
                clientConfigManager.confirmState();
            }
            catch (Throwable e){
                logger.error("Error during ConfigManager confirmState.", e);
            }
        }, 30, 45, TimeUnit.SECONDS);

        scriptExecutorService.scheduleAtFixedRate(() -> {
            try {
                scriptManager.loop();
            }
            catch (Throwable e){
                logger.error("Error during script manager confirmState.", e);
            }
        }, 3, 1, TimeUnit.SECONDS);

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

    @SuppressWarnings("unchecked")
    public WorldDataResult requestWorldData(){
        try{
            return send(new MessagePackage(MessagePackage.Type.REQUEST_WORLD_DATA, MessagePackage.SERVER))
                    .waitForResponse(30, TimeUnit.SECONDS)
                    .getResponse()
                    .map(messagePackage -> {
                        return messagePackage.getBodyAs(WorldDataResult.class);
                    })
                    .orElse(null);
        }
        catch (Throwable e){
            e.printStackTrace();
            System.out.println();
        }
        return null;
    }

    public Optional<MessagePackage> confirmState(){
        RSAccount rsAccount = getRsAccountManager().getRsAccount();
        String rev = getBotClientConfig() != null ? getBotClientConfig().getRev() : null;

        Optional<MessagePackage> response = send(new MessagePackage(MessagePackage.Type.CONFIRM_CLIENT_STATE, MessagePackage.SERVER)
                .setBody(0, rev)
                .setBody(1, rsAccount != null ? rsAccount.getID() : null)
        ).waitForResponse(30, TimeUnit.SECONDS).getResponse();

        logger.debug("Confirmed state. {}/{}, {}/{}", rev, response.map(messagePackage -> messagePackage.getBodyAs(0, Boolean.class)).orElse(false), rsAccount, response.map(messagePackage -> messagePackage.getBodyAs(1, Boolean.class)).orElse(false));

        return response;
    }

    public boolean updateClientConfig(BotClientConfig botClientConfig, boolean serializeNull) {
        synchronized (BotClientConfigManager.LOCK){
            return send(new MessagePackage(MessagePackage.Type.UPDATE_CLIENT_CONFIG, MessagePackage.SERVER)
                    .setBody(0, botClientConfig)
                    .setBody(1, serializeNull)
            )
                    .waitForResponse(30, TimeUnit.SECONDS)
                    .getResponse().map(messagePackage -> messagePackage.getBodyAs(boolean.class)).orElse(false);
        }
    }

    public void updateClientStateNoResponse(BotClientState botClientState, boolean serializeNull) {
        send(new MessagePackage(MessagePackage.Type.UPDATE_CLIENT_STATE, MessagePackage.SERVER)
                .setBody(0, botClientState)
                .setBody(1, serializeNull)
        );
    }

    public boolean updateClientState(BotClientState botClientState, boolean serializeNull) {
        return send(new MessagePackage(MessagePackage.Type.UPDATE_CLIENT_STATE, MessagePackage.SERVER)
                .setBody(0, botClientState)
                .setBody(1, serializeNull)

        )
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse().map(messagePackage -> messagePackage.getBodyAs(boolean.class)).orElse(false);
    }

    @SuppressWarnings("unchecked")
    public List<RSAccount> requestRSAccounts(boolean filterUnassignable) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_ACCOUNTS, MessagePackage.SERVER).setBody(filterUnassignable))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(RSAccount[].class)))
                .orElse(Collections.EMPTY_LIST);
    }

    @SuppressWarnings("unchecked")
    public List<Tag> requestTags(String title) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_TAGS, MessagePackage.SERVER).setBody(title))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(Tag[].class)))
                .orElse(Collections.EMPTY_LIST);
    }

    public boolean requestTagAccount(RSAccount account, Tag tag) {
        return send(new MessagePackage(MessagePackage.Type.ADD_RS_ACCOUNT_TAG, MessagePackage.SERVER)
                .setBody(0, true)
                .setBody(1, account)
                .setBody(2, tag)
        ).waitForResponse(30, TimeUnit.SECONDS).getResponse().map(messagePackage -> messagePackage.getBodyAs(boolean.class)).orElse(false);
    }

    public boolean requestAccountAssignment(RSAccount account, boolean force) {
        return send(
                new MessagePackage(MessagePackage.Type.REQUEST_ACCOUNT_ASSIGNMENT, MessagePackage.SERVER)
                        .setBody(0, account == null ? null : account.getID())
                        .setBody(1, force))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(false);
    }

    public boolean isAccountAssigned(RSAccount rsAccount) {
        return send(new MessagePackage(MessagePackage.Type.CHECK_ACCOUNT_ASSIGNMENT, MessagePackage.SERVER).setBody(rsAccount.getID()))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(true);
    }

    public List<BotClient> requestBotClients() {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_BOT_CLIENTS, MessagePackage.SERVER))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(BotClient[].class)))
                .orElse(Collections.emptyList());
    }

    public RemoteScriptTask.StartResponse requestRemoteTaskStart(String destinationKey, RemoteScriptTask.StartRequest startRequest) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_REMOTE_TASK_START, destinationKey).setBody(startRequest))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(RemoteScriptTask.StartResponse.class))
                .orElse(null);
    }

    public Optional<Script> requestScript(String scriptID) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT, MessagePackage.SERVER).setBody(scriptID))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(Script.class));
    }

    public Optional<Tag> requestTag(String tagID) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_TAG, MessagePackage.SERVER).setBody(tagID))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse().map(messagePackage -> messagePackage.getBodyAs(Tag.class));
    }

    public void requestStatusSet(String key, String status) {
        send(new MessagePackage(MessagePackage.Type.REQUEST_STATUS_SET, MessagePackage.SERVER)
                .setBody(0, key)
                .setBody(1, status));
    }

    public void requestStatusClear() {
        send(new MessagePackage(MessagePackage.Type.REQUEST_CLEAR_STATUS, MessagePackage.SERVER));
    }

    public Optional<ScriptVersion> requestScriptVersion(String scriptID, String scriptVersionID) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT_VERSION, MessagePackage.SERVER)
                .setBody(0, scriptID)
                .setBody(1, scriptVersionID)
        )
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(ScriptVersion.class));
    }

    public MessageResponse send(MessagePackage messagePackage) {
        return connection.send(messagePackage);
    }

    public void respond(MessagePackage init, MessagePackage response) {
        response.setResponseToKey(init.getResponseKey());
        send(response);
    }

    public BotControlConnection getConnection() {
        return connection;
    }

    @Override
    public void handleException(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext) {
        throwable.printStackTrace();
    }

    public abstract void sendClientState();

    public abstract Object createInstanceOfScript(ScriptNode scriptRunConfig);

    public abstract void destroyInstanceOfScript(Object scriptInstance);

    public abstract boolean evaluate(Object evaluator);

    public abstract boolean isSignedIn(RSAccount rsAccount);

    public abstract void sendInGameMessage(String messagePackageBodyAs);

    public abstract Integer getCurrentWorld();

    public abstract void hopToWorld(int world);

    public abstract BufferedImage getScreenCapture();

    public abstract boolean executeLoginHandler();

    public boolean isSignedIn() {
        RSAccount rsAccount = getRsAccountManager().getRsAccount();
        return rsAccount != null && isSignedIn(rsAccount);
    }

    private synchronized void interceptSystemOut() {
        PrintStream out = System.out;
        if (out instanceof RemotePrintStream) ((RemotePrintStream) out).setBotControl(this);
        else System.setOut(new RemotePrintStream(this, out));

        PrintStream err = System.err;
        if (err instanceof RemotePrintStream) ((RemotePrintStream) err).setBotControl(this);
        else System.setErr(new RemotePrintStream(this, err));
    }

    public void stop() {
        scheduledExecutorService.shutdownNow();
        connection.stop();
    }

    public BotClientConfig getBotClientConfig() {
        return clientConfigManager.getCurrentConfig();
    }
}
