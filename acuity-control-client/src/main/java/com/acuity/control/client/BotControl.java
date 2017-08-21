package com.acuity.control.client;

import com.acuity.control.client.accounts.RSAccountManager;
import com.acuity.control.client.breaks.BreakManager;
import com.acuity.control.client.proxies.ProxyManager;
import com.acuity.control.client.scripts.ScriptManager;
import com.acuity.control.client.util.RemotePrintStream;
import com.acuity.control.client.websockets.response.MessageResponse;
import com.acuity.db.domain.common.ClientType;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.domain.vertex.impl.scripts.ScriptQueue;
import com.acuity.db.domain.vertex.impl.scripts.ScriptRunConfig;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.omg.PortableInterceptor.Interceptor;

import java.io.PrintStream;

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
        interceptSystemOut();
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

    public MessageResponse updateScriptQueue(ScriptQueue scriptQueue) {
        if (scriptQueue == null) return null;
        return send(new MessagePackage(MessagePackage.Type.UPDATE_SCRIPT_QUEUE, MessagePackage.SERVER).setBody(scriptQueue));
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

    public abstract Object createInstanceOfScript(ScriptRunConfig scriptRunConfig);

    public abstract void destroyInstanceOfScript(Object scriptInstance);

    public void onLoop() {
        scriptManager.onLoop();
    }

    public synchronized void interceptSystemOut(){
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
