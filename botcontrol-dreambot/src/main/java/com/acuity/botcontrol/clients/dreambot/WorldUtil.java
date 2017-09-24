package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.BotControl;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClient;
import com.acuity.db.domain.vertex.impl.bot_clients.BotClientState;
import org.dreambot.api.methods.MethodContext;

import java.util.*;

/**
 * Created by Zach on 9/24/2017.
 */
public class WorldUtil {

    public static Map<Integer, Integer> getWorldBotPopulations(MethodContext methodContext, BotControl botControl){
        Map<Integer, Integer> worldPop = new HashMap<>();

        methodContext.getWorlds().all().forEach(world -> worldPop.put(world.getRealID(), 0));
        botControl.requestBotClients().stream()
                .map(BotClient::getClientState)
                .map(BotClientState::getCurrentWorld)
                .filter(Objects::nonNull)
                .forEach(world -> worldPop.put(world, worldPop.getOrDefault(world, 0) + 1));

        return worldPop;
    }
}
