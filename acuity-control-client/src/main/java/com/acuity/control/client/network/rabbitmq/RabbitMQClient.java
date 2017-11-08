package com.acuity.control.client.network.rabbitmq;

import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.acuity.db.util.Json;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RabbitMQClient {

    private static final Logger logger  = LoggerFactory.getLogger(RabbitMQClient.class);

    private Executor executor = Executors.newSingleThreadExecutor();
    private Channel channel;

    public void start(){
        executor.execute(() -> {
            while (channel == null){
                try {
                    channel =  createChannel("statsTemp", "123123").orElse(null);
                }
                catch (Throwable e){
                    logger.warn("Failed to connect.");
                }
            }
            logger.info("Connected to Acuity RabbitMQ server. {}", isConnected());
            sendEvent(new MessagePackage(12, "asdasdasd"));
        });
    }

    public void sendEvent(MessagePackage event){
        if (isConnected()){
            try {
                channel.basicPublish("acuity.stats.events", "", null, Json.GSON.toJson(event).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean isConnected(){
        return channel != null && channel.isOpen();
    }

    private Optional<Channel> createChannel(String username, String password){
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("174.53.192.24");
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setRequestedHeartbeat(10);
            Connection connection = factory.newConnection();
            return Optional.ofNullable(connection.createChannel());
        } catch (Throwable e) {
            logger.error("Error during creating RabbitMQ createChannel.", e);
        }

        return Optional.empty();
    }


    public static void main(String[] args) {
        new RabbitMQClient().start();
    }
}
