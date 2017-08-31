package com.acuity.control.client;

import com.acuity.control.client.accounts.RSAccountManager;
import com.acuity.control.client.breaks.BreakManager;
import com.acuity.control.client.proxies.ProxyManager;
import com.acuity.control.client.scripts.ScriptManager;
import com.acuity.control.client.util.RemotePrintStream;
import com.acuity.control.client.websockets.response.MessageResponse;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClient;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.message_package.data.RemoteScript;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.Script;
import com.acuity.db.domain.vertex.impl.scripts.ScriptRoutine;
import com.acuity.db.domain.vertex.impl.scripts.ScriptStartupConfig;
import com.acuity.db.domain.vertex.impl.scripts.ScriptVersion;
import com.acuity.db.domain.vertex.impl.tag.Tag;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zach on 8/20/2017.
 */
public abstract class BotControl implements SubscriberExceptionHandler {

    private EventBus eventBus = new EventBus(this);

    private ScriptManager scriptManager = new ScriptManager(this);
    private BreakManager breakManager = new BreakManager(this);
    private RSAccountManager rsAccountManager = new RSAccountManager(this);
    private ProxyManager proxyManager = new ProxyManager(this);
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private BotControlConnection connection;

    public BotControl(String host, ClientType clientType) {
        this.connection = new BotControlConnection(this, host, clientType);

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (connection.isConnected()) {
                try {
                    sendClientState();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
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

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public MessageResponse updateCurrentScriptRunConfig(ScriptStartupConfig runConfig) {
        return send(new MessagePackage(MessagePackage.Type.UPDATE_CURRENT_SCRIPT_RUN_CONFIG, MessagePackage.SERVER).setBody(runConfig));
    }

    @SuppressWarnings("unchecked")
    public List<RSAccount> getRSAccounts() {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_ACCOUNTS, MessagePackage.SERVER))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(RSAccount[].class)))
                .orElse(Collections.EMPTY_LIST);
    }

    @SuppressWarnings("unchecked")
    public List<Tag> getTags(String title) {
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

    public RemoteScript.StartResponse requestRemoteScriptStart(String destinationKey, RemoteScript.StartRequest startRequest) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_REMOTE_SCRIPT_QUEUE, destinationKey).setBody(startRequest))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(RemoteScript.StartResponse.class))
                .orElse(null);
    }

    public Optional<Script> requestScript(String scriptID) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT, MessagePackage.SERVER).setBody(scriptID))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(Script.class));
    }

    public Optional<ScriptVersion> requestScriptVersion(String scriptID, String scriptVersionID) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT, MessagePackage.SERVER)
                .setBody(0,scriptID)
                .setBody(1, scriptVersionID)
        )
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(ScriptVersion.class));
    }

    public MessageResponse updateScriptQueue(ScriptRoutine scriptQueue) {
        if (scriptQueue == null) return null;
        return send(new MessagePackage(MessagePackage.Type.UPDATE_SCRIPT_QUEUE, MessagePackage.SERVER).setBody(scriptQueue));
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

    public abstract Object createInstanceOfScript(ScriptStartupConfig scriptRunConfig);

    public abstract void destroyInstanceOfScript(Object scriptInstance);

    public abstract boolean evaluate(Object evaluator);

    public void onLoop() {
        try {
            scriptManager.onLoop();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            rsAccountManager.onLoop();
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
}
