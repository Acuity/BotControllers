package com.acuity.control.client.managers.proxies;

import com.acuity.common.util.IPUtil;
import com.acuity.control.client.BotControl;
import com.acuity.control.client.util.ProxyUtil;
import com.acuity.db.domain.vertex.impl.proxy.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/21/2017.
 */
public class ProxyManager {

    private static Logger logger = LoggerFactory.getLogger(ProxyManager.class);

    private int maxBotsPerIP = 10;

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
            Map<String, Double> ipData = botControl.getRemote().requestIPData().orElse(null);
            if (ipData != null){
                double ipBotCount = ipData.getOrDefault(ip, 1d);
                if (ipBotCount > maxBotsPerIP){
                    logger.warn("To many bots on IP. {}, {}", ip, ipBotCount);
                    Set<Proxy> viableProxies = botControl.getRemote().requestProxies().orElse(Collections.emptyList()).stream()
                            .filter(proxy -> ipData.getOrDefault(proxy.getHost(), 0d) < maxBotsPerIP)
                            .collect(Collectors.toSet());

                    logger.debug("Viable proxies under count {}. {}", maxBotsPerIP, viableProxies.size());
                    if (viableProxies.size() > 0){
                        setProxy(viableProxies.stream().findAny().orElse(null));
                    }
                }
            }
        }
    }

    public int getMaxBotsPerIP() {
        return maxBotsPerIP;
    }

    public ProxyManager setMaxBotsPerIP(int maxBotsPerIP) {
        if (maxBotsPerIP > 10) throw new IllegalArgumentException("Max bots per IP must be 10 or under.");
        this.maxBotsPerIP = maxBotsPerIP;
        return this;
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
