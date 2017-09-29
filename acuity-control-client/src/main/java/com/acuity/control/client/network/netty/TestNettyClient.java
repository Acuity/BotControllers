package com.acuity.control.client.network.netty;

import com.acuity.common.network.io.FODecoder;
import com.acuity.common.network.io.FOEncoder;
import com.acuity.control.client.network.NetworkEvent;
import com.acuity.control.client.network.NetworkInterface;
import com.acuity.control.client.network.response.MessageResponse;
import com.acuity.control.client.network.response.ResponseTracker;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.util.Json;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Zachary Herridge on 9/29/2017.
 */
public class TestNettyClient implements NetworkInterface, SubscriberExceptionHandler{

    private ResponseTracker tracker = new ResponseTracker();
    private EventBus eventBus = new EventBus(this);

    private EventLoopGroup clientGroup = new NioEventLoopGroup();
    private ChannelHandlerContext context;
    private Executor executor = Executors.newCachedThreadPool();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void start() {
        new Thread(() -> {
            Bootstrap bootstrap = new Bootstrap();
            Logger logger = LoggerFactory.getLogger("Client");
            bootstrap.group(clientGroup)
                    .channel(NioSocketChannel.class)
                    .remoteAddress("localhost", 2052)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new FOEncoder(),
                                    new FODecoder(),
                                    new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
                                            logger.info("channelUnregistered");
                                        }

                                        @Override
                                        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                                            logger.info("channelRegistered");
                                            context = ctx;
                                        }

                                        @Override
                                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                            logger.info("channelInactive");
                                            eventBus.post(new NetworkEvent.Closed());
                                        }

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            logger.info("channelActive");
                                            executor.execute(() -> eventBus.post(new NetworkEvent.Opened()));
                                        }

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            logger.info("channelRead. {}", msg);
                                            if (msg != null && msg instanceof String){
                                                String in = (String) msg;
                                                if (in.isEmpty()) return;

                                                try {
                                                    MessagePackage messagePackage = Json.GSON.fromJson(in, MessagePackage.class);
                                                    if (messagePackage.getResponseToKey() != null){
                                                        MessageResponse response = getResponseTracker().getCache().getIfPresent(messagePackage.getResponseToKey());
                                                        if (response != null) {
                                                            response.setResponse(messagePackage);
                                                            getResponseTracker().getCache().invalidate(messagePackage.getResponseToKey());
                                                        }
                                                    }

                                                    executor.execute(() -> getEventBus().post(messagePackage));
                                                }
                                                catch (Throwable e){
                                                    logger.error("Error during handling MessagePackage.", e);
                                                    logger.info("Error json. {}", in);
                                                }
                                            }
                                        }
                                    }
                            );
                        }
                    });

            bootstrap.connect();


        }).start();
    }

    @Override
    public void shutdown() {
        clientGroup.shutdownGracefully();
    }

    @Override
    public void disconnect() {
        context.channel().disconnect();
    }

    @Override
    public void send(MessagePackage messagePackage) {
        if (messagePackage != null){
            String json = Json.GSON.toJson(messagePackage);
            if (json != null && !json.isEmpty()){
                context.writeAndFlush(json);
            }
        }
    }

    @Override
    public ResponseTracker getResponseTracker() {
        return tracker;
    }

    @Override
    public boolean isConnected() {
        return context != null && context.channel().isActive();
    }

    @Override
    public void handleException(Throwable throwable, SubscriberExceptionContext subscriberExceptionContext) {
        throwable.printStackTrace();
    }
}
