package com.acuity.control.client;

import com.acuity.control.client.accounts.RSAccountManager;
import com.acuity.control.client.breaks.BreakManager;
import com.acuity.control.client.proxies.ProxyManager;
import com.acuity.control.client.scripts.ScriptManager;
import com.acuity.control.client.websockets.response.MessageResponse;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.scripts.ScriptRunConfig;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

/**
 * Created by Zach on 8/20/2017.
 */
public class BotControl implements SubscriberExceptionHandler{

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

    public MessageResponse requestScript(ScriptRunConfig runConfig){
        return send(new MessagePackage(MessagePackage.Type.REQUEST_SCRIPT_CHANGE, MessagePackage.SERVER).setBody(runConfig));
    }

    public MessageResponse send(MessagePackage messagePackage){
        return connection.send(messagePackage);
    }

    public BotControlConnection getConnection() {
        return connection;
    }

    @Override
    public void handleException(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext) {
        throwable.printStackTrace();
    }

    public void onLoop() {
        scriptManager.onLoop();
    }

    public void stop() {
        connection.stop();
    }
}
