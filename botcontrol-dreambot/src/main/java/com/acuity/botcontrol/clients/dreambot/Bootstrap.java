package com.acuity.botcontrol.clients.dreambot;

import org.dreambot.Boot;
import org.dreambot.core.Instance;
import org.dreambot.core.InstancePool;

import static org.dreambot.api.methods.MethodProvider.sleep;

/**
 * Created by Zachary Herridge on 9/25/2017.
 */
public class Bootstrap {

    public static void main(String[] args) {
        Boot.main(new String[]{});

        while (InstancePool.getAll().size() == 0) {
            sleep(1000);
        }

        Instance instance = InstancePool.getAll().stream().findFirst().orElse(null);

        while (instance.getClient().getGameStateID() < 10){
            sleep(1000);
        }

        instance.getScriptManager().start(DreambotControlScript.class);
    }
}
