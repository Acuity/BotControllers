package com.acuity.control.client.world;

import com.acuity.common.world_data_parser.WorldData;
import com.acuity.common.world_data_parser.WorldDataResult;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.common.world.RSWorldSelector;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 9/25/2017.
 */
public class WorldManager {

    private static final Logger logger = LoggerFactory.getLogger(WorldManager.class);

    private BotControl botControl;
    private LocalDateTime lastCheck = LocalDateTime.MIN;
    private static Predicate<WorldData> predicate = entry -> !entry.isMembers();

    public WorldManager(BotControl botControl) {
        this.botControl = botControl;
    }


    public boolean onLoop(){
        if (!botControl.isSignedIn()) return false;

        RSWorldSelector rsWorldSelector = botControl.getBotClientConfig().getScriptSelector().getRsWorldSelector();
        rsWorldSelector = botControl.getScriptManager().getExecutionNode().map(ScriptNode::getRsWorldSelector).orElse(rsWorldSelector);

        Integer currentWorld = botControl.getCurrentWorld();
        if (currentWorld == null) return false;

        if (lastCheck.isBefore(LocalDateTime.now().minusSeconds(25))){
            WorldDataResult worldData = botControl.requestWorldData();
            worldData.zip();

            int currentWorldBotPopulation = worldData.getWorldBotPopulation().getOrDefault(currentWorld, Integer.MAX_VALUE) - 1;

            RSWorldSelector finalRsWorldSelector = rsWorldSelector;
            List<WorldData> betterWorlds = worldData.getWorldData().stream()
                    .filter(entry -> entry.getBotPopulation() < currentWorldBotPopulation)
                    .filter(entry -> {
                        if (finalRsWorldSelector == null || !finalRsWorldSelector.isEnabled()) return true;

                        if (entry.isMembers() && !finalRsWorldSelector.isP2p()) return false;
                        if (!entry.isMembers() && !finalRsWorldSelector.isF2p()) return false;

                        return true;
                    })
                    .sorted((o1, o2) -> Double.compare(o2.getPopulation(), o1.getPopulation()))
                    .collect(Collectors.toList());

            logger.info("Viable worlds vs current pop. {}, {}", currentWorldBotPopulation, betterWorlds);

            if (betterWorlds.size() > 0){
                int world = betterWorlds.get(0).getWorld();
                logger.info("Found better world. {}",world );
                botControl.hopToWorld(world);
            }

            lastCheck = LocalDateTime.now();
        }
        return false;
    }

}
