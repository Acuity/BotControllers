package com.acuity.control.client.network.netty;

import com.acuity.control.client.network.websockets.WClientEvent;
import com.acuity.control.client.network.websockets.response.MessageResponse;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.util.Json;
import com.google.common.eventbus.EventBus;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zach on 9/27/2017.
 */
@ChannelHandler.Sharable
public class NettyClientHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    private NettyClient client;
    private ChannelHandlerContext context;

    private Executor executor = Executors.newSingleThreadExecutor();

    public NettyClientHandler(NettyClient nettyClient) {
        this.client = nettyClient;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel active.");
        super.channelActive(ctx);
        executor.execute(() -> client.getEventBus().post(new WClientEvent.Opened()));
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel registered.");
        super.channelRegistered(ctx);
        context = ctx;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String in) throws Exception {
        if (in == null || in.isEmpty()) return;

        try {
            MessagePackage messagePackage = Json.GSON.fromJson(in, MessagePackage.class);
            if (messagePackage.getResponseToKey() != null){
                MessageResponse response = client.getResponseTracker().getCache().getIfPresent(messagePackage.getResponseToKey());
                if (response != null) {
                    response.setResponse(messagePackage);
                    client.getResponseTracker().getCache().invalidate(messagePackage.getResponseToKey());
                }
            }

            executor.execute(() -> client.getEventBus().post(messagePackage));
        }
        catch (Throwable e){
            logger.error("Error during handling MessagePackage.", e);
            logger.info("Error json. {}", in);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel inactive.");
        super.channelInactive(ctx);
        client.getEventBus().post(new WClientEvent.Closed(0, null, false));
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel unregistered.");
        super.channelUnregistered(ctx);
        scheduleReconnect(ctx);
    }

    private void scheduleReconnect(final ChannelHandlerContext ctx){
        final EventLoop loop = ctx.channel().eventLoop();
        if (!loop.isShutdown() && !loop.isShuttingDown()){
            loop.schedule(() -> {
                logger.info("Starting reconnect.");
                client.configureBootstrap(new Bootstrap(), loop).connect();
            }, 5, TimeUnit.SECONDS);
        }
    }

    public void send(MessagePackage messagePackage) {
        try {
            String json = Json.GSON.toJson(messagePackage);
            if (context != null) context.writeAndFlush(json);
        }
        catch (Throwable e){
            logger.error("Error during sending MessagePackage.", e);
        }
    }

    public void close() {
        try {
            if (context != null) context.channel().close();
        }
        catch (Throwable e){
            logger.error("Error during closing.", e);
        }
    }

    public boolean isConnected() {
        return context != null && context.channel().isOpen();
    }
}
