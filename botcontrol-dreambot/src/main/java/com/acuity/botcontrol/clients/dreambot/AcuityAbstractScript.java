package com.acuity.botcontrol.clients.dreambot;

import com.acuity.control.client.BotControl;
import com.acuity.control.client.managers.scripts.RemoteScriptStartCheck;
import com.acuity.db.domain.vertex.impl.message_package.MessagePackage;
import org.dreambot.api.methods.world.World;
import org.dreambot.api.script.AbstractScript;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/28/2017.
 */
public abstract class AcuityAbstractScript extends AbstractScript implements RemoteScriptStartCheck {

    private BotControl botControl;

    public BotControl getBotControl() {
        return botControl;
    }

    @Override
    public boolean isAcceptingTasks(){
        return true;
    }

    public abstract void onMessagePackage(MessagePackage messagePackage);

    public int getBestWorld(Predicate<World> filter){
        Map<Integer, Integer> worldBotPopulations = WorldUtil.getWorldBotPopulations(this, getBotControl());

        List<World> worldsRanked = getWorlds()
                .f2p()
                .stream()
                .filter(filter)
                .sorted(Comparator.comparingInt(o -> worldBotPopulations.getOrDefault(o.getRealID(), Integer.MAX_VALUE)))
                .collect(Collectors.toList());

        World bestWorld = worldsRanked.get(0);

        int currentWorld = getClient().getCurrentWorld();
        Integer currentWorldBotPopulation = Integer.MAX_VALUE;

        try{
            World world = getWorlds().getWorld(currentWorld);
            if (world != null && !filter.test(world)) world = null;
            if (world != null){
                currentWorldBotPopulation = worldBotPopulations.getOrDefault(world.getRealID(), Integer.MAX_VALUE);
            }
        }
        catch (Throwable e){
            e.printStackTrace();
        }

        if (currentWorldBotPopulation != null && worldBotPopulations.getOrDefault(bestWorld.getID(), Integer.MAX_VALUE) + 1 >= currentWorldBotPopulation){
            return currentWorld;
        }

        return bestWorld.getRealID();
    }
}
