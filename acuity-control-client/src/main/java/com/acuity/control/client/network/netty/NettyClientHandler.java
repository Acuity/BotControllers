package com.acuity.control.client.network.netty;

import com.acuity.control.client.network.NetworkEvent;
import com.acuity.control.client.network.response.MessageResponse;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.util.Json;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zach on 9/27/2017.
 */
@ChannelHandler.Sharable
public class NettyClientHandler extends ChannelInboundHandlerAdapter {

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
        executor.execute(() -> client.getEventBus().post(new NetworkEvent.Opened()));
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel registered.");
        super.channelRegistered(ctx);
        context = ctx;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg != null && msg instanceof String){
            String in = (String) msg;

            if (in.isEmpty()) return;

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

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel inactive.");
        super.channelInactive(ctx);
        client.getEventBus().post(new NetworkEvent.Closed());
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
