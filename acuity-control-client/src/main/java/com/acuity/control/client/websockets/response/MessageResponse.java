package com.acuity.control.client.websockets.response;

import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Zachary Herridge on 8/15/2017.
 */
public class MessageResponse {

    private volatile MessagePackage response;

    public void setResponse(MessagePackage response) {
        this.response = response;
    }

    public Optional<MessagePackage> getResponse() {
        return Optional.ofNullable(response);
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
        return this;
    }
}
