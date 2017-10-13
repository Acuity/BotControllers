package com.acuity.control.client.managers.proxies;

import com.acuity.common.util.IPUtil;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.util.ProxyUtil;
import com.acuity.db.domain.vertex.impl.proxy.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ProxyManager {

    private static Logger logger = LoggerFactory.getLogger(ProxyManager.class);

    private Proxy proxy;
    private boolean proxyConfirmed = false;
    private BotControl botControl;

    private boolean autoBalance = true;

    public ProxyManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public void loop(){
        String ip = IPUtil.getIP().orElse(null);

        if (ip == null) return;

        if (!proxyConfirmed && proxy != null) {
            logger.info("Confirming proxy. {}, {}", proxy, ip);
            proxyConfirmed = Objects.equals(proxy.getHost(), ip);

            if (!proxyConfirmed) {
                setProxy(proxy);
                return;
            }
        }

        if (autoBalance){
            Map<String, Long> ipData = botControl.getRemote().requestIPData().orElse(null);
            if (ipData != null){
                long ipBotCount = ipData.getOrDefault(ip, 1L);
                if (ipBotCount > 10){
                    logger.warn("To many bots on IP. {}, {}", ip, ipBotCount);
                }
            }
        }
    }

    public ProxyManager setAutoBalance(boolean autoBalance) {
        this.autoBalance = autoBalance;
        return this;
    }

    public synchronized void setProxy(Proxy proxy){
        logger.info("Proxy changing. new={}, old={}", proxy, this.proxy);
        this.proxy = proxy;
        ProxyUtil.setSocksProxy(proxy, botControl);
        botControl.getStateManager().clearIPGrabTimestamp().send();
        botControl.getClientInterface().closeRSSocket();
        proxyConfirmed = false;
    }

    public Proxy getProxy() {
        return proxy;
    }
}
