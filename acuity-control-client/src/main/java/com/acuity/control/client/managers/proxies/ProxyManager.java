package com.acuity.control.client.managers.proxies;

import com.acuity.control.client.BotControl;
import com.acuity.control.client.BotControlEvent;
import com.acuity.control.client.util.ProxyUtil;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientConfig;
import com.acuity.db.domain.vertex.impl.proxy.Proxy;

import java.util.Objects;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ProxyManager {

    private Proxy proxy;
    private BotControl botControl;

    public ProxyManager(BotControl botControl) {
        this.botControl = botControl;
    }

    private void setProxy(Proxy proxy){
        this.proxy = proxy;
        ProxyUtil.setSocksProxy(proxy, botControl);
        botControl.getStateManager().clearIPGrabTimestamp().send();
        botControl.getEventBus().post(new BotControlEvent.ProxyUpdated());
    }

    public Proxy getProxy() {
        return proxy;
    }
}
