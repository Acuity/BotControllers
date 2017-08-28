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
import com.acuity.db.domain.vertex.impl.message_package.data.ScriptStartRequest;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.ScriptQueue;
import com.acuity.db.domain.vertex.impl.scripts.ScriptRunConfig;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zach on 8/20/2017.
 */
public abstract class BotControl implements SubscriberExceptionHandler{

    private EventBus eventBus = new EventBus(this);

    private ScriptManager scriptManager = new ScriptManager(this);
    private BreakManager breakManager = new BreakManager(this);
    private RSAccountManager rsAccountManager = new RSAccountManager(this);
    private ProxyManager proxyManager = new ProxyManager(this);

    private BotControlConnection connection;

    public BotControl(String host, ClientType clientType) {
        this.connection = new BotControlConnection(this, host, clientType);
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

    public MessageResponse updateCurrentScriptRunConfig(ScriptRunConfig runConfig){
        return send(new MessagePackage(MessagePackage.Type.UPDATE_CURRENT_SCRIPT_RUN_CONFIG, MessagePackage.SERVER).setBody(runConfig));
    }

    public boolean requestAccountAssignment(RSAccount account) {
        return send(new MessagePackage(MessagePackage.Type.REQUEST_ACCOUNT_ASSIGNMENT, MessagePackage.SERVER).setBody(account.getID()))
                .waitForResponse(10, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(false);
    }

    @SuppressWarnings("unchecked")
    public List<RSAccount> getRSAccounts(){
        return send(new MessagePackage(MessagePackage.Type.REQUEST_ACCOUNTS, MessagePackage.SERVER))
                .waitForResponse(10, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(RSAccount[].class)))
                .orElse(Collections.EMPTY_LIST);
    }

    public boolean isAccountAssigned(RSAccount rsAccount){
        return send(new MessagePackage(MessagePackage.Type.CHECK_ACCOUNT_ASSIGNMENT, MessagePackage.SERVER).setBody(rsAccount.getID()))
                .waitForResponse(10, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(true);
    }

    public List<BotClient> requestBotClients(){
        return send(new MessagePackage(MessagePackage.Type.REQUEST_BOT_CLIENTS, MessagePackage.SERVER))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> Arrays.asList(messagePackage.getBodyAs(BotClient[].class)))
                .orElse(Collections.emptyList());
    }

    public boolean requestRemoteScriptStart(String destinationKey, ScriptStartRequest startRequest){
        return send(new MessagePackage(MessagePackage.Type.REQUEST_REMOTE_SCRIPT_QUEUE, destinationKey).setBody(startRequest))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(boolean.class))
                .orElse(false);
    }

    public Optional<ScriptRunConfig> requestScriptRunConfig(String scriptID, String scriptVersion){
        return send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT_RUN_CONFIG, MessagePackage.SERVER).setBody(new String[]{scriptID, scriptVersion}))
                .waitForResponse(30, TimeUnit.SECONDS)
                .getResponse()
                .map(messagePackage -> messagePackage.getBodyAs(ScriptRunConfig.class));
    }

    public MessageResponse updateScriptQueue(ScriptQueue scriptQueue) {
        if (scriptQueue == null) return null;
        return send(new MessagePackage(MessagePackage.Type.UPDATE_SCRIPT_QUEUE, MessagePackage.SERVER).setBody(scriptQueue));
    }

    public MessageResponse send(MessagePackage messagePackage){
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

    public abstract Object createInstanceOfScript(ScriptRunConfig scriptRunConfig);

    public abstract void destroyInstanceOfScript(Object scriptInstance);

    public void onLoop() {
        try {
            scriptManager.onLoop();
        }
        catch (Throwable e){
            e.printStackTrace();
        }
        try {
            rsAccountManager.onLoop();
        }
        catch (Throwable e){
            e.printStackTrace();
        }
    }

    private synchronized void interceptSystemOut(){
        PrintStream out = System.out;
        if (out instanceof RemotePrintStream) ((RemotePrintStream) out).setBotControl(this);
        else System.setOut(new RemotePrintStream(this, out));

        PrintStream err = System.err;
        if (err instanceof RemotePrintStream) ((RemotePrintStream) err).setBotControl(this);
        else System.setErr(new RemotePrintStream(this, err));
    }

    public void stop() {
        connection.stop();
    }
}
