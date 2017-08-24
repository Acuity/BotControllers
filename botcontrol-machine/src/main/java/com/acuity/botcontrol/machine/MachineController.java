package com.acuity.botcontrol.machine;

import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import com.google.common.eventbus.Subscribe;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zach on 8/19/2017.
 */
public class MachineController {

    private MachineConnection connection;
    private ThreadPoolExecutor threadPoolExecutor;

    public MachineController() {
        connection = new MachineConnection("localhost");
        connection.getEventBus().register(this);
        threadPoolExecutor = new ThreadPoolExecutor(0, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @Subscribe
    public void onMessage(MessagePackage messagePackage){
        if (messagePackage.getMessageType() == MessagePackage.Type.RUN_SYSTEM_COMMAND){
            String command = messagePackage.getBodyAs(String.class);
            threadPoolExecutor.execute(() -> {
                try {
                    Runtime.getRuntime().exec(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    public static void main(String[] args) {
        new MachineController();
    }
}
