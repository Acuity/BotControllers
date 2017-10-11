package com.acuity.control.client.network.response;

import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Zachary Herridge on 8/15/2017.
 */
public class MessageResponse {

    private static final Logger logger = LoggerFactory.getLogger(MessageResponse.class);

    private volatile MessagePackage response;
    private String responseKey;

    public MessageResponse(String responseKey) {
        this.responseKey = responseKey;
    }

    public void setResponse(MessagePackage response) {
        this.response = response;
    }

    public Optional<MessagePackage> getResponse() {
        return Optional.ofNullable(response);
    }

    public <T> Optional<T> getBodyAs(int index, Class<T> clazz){
        return getResponse().map(messagePackage -> messagePackage.getBodyAs(index, clazz));
    }

    public <T> Optional<T> getBodyAs(Class<T> clazz){
        return getBodyAs(0, clazz);
    }

    public void ifPresent(Consumer<MessagePackage> consumer) {
        if (response != null) consumer.accept(response);
    }

    public MessageResponse waitForResponse(int duration, TimeUnit timeUnit){
        long end = System.currentTimeMillis() + timeUnit.toMillis(duration);
        while (response == null && System.currentTimeMillis() < end){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (response == null) {
            logger.error("Timed out - {}.", responseKey);
        }

        return this;
    }
}
