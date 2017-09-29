package com.acuity.control.client.managers.world;

import com.acuity.common.world_data_parser.WorldData;
import com.acuity.common.world_data_parser.WorldDataResult;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.common.RSWorldSelector;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 9/25/2017.
 */
public class WorldManager {

    private static final Logger logger = LoggerFactory.getLogger(WorldManager.class);

    private BotControl botControl;
    private LocalDateTime lastCheck = LocalDateTime.MIN;

    public WorldManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public boolean onLoop(){
        if (!botControl.isSignedIn()) return false;

        RSWorldSelector rsWorldSelector = botControl.getBotClientConfig().getScriptSelector().getRsWorldSelector();
        rsWorldSelector = botControl.getScriptManager().getExecutionNode().map(ScriptNode::getRsWorldSelector).orElse(rsWorldSelector);

        if (rsWorldSelector != null && !rsWorldSelector.isEnabled()) return false;

        Integer currentWorld = botControl.getCurrentWorld();
        if (currentWorld == null) return false;

        if (lastCheck.isBefore(LocalDateTime.now().minusSeconds(10))){
            WorldDataResult worldData = botControl.requestWorldData();
            worldData.zip();

            int currentWorldBotPopulation = worldData.getWorldBotPopulation().getOrDefault(currentWorld, Integer.MAX_VALUE) - 2;

            RSWorldSelector finalRsWorldSelector = rsWorldSelector;

            List<WorldData> betterWorlds = worldData.getWorldData().stream()
                    .filter(entry -> entry.getBotPopulation() < currentWorldBotPopulation)
                    .filter(entry -> {
                        if (finalRsWorldSelector == null) return true;

                        if (entry.isMembers() && !finalRsWorldSelector.isP2p()) return false;
                        if (!entry.isMembers() && !finalRsWorldSelector.isF2p()) return false;
                        if (entry.isDangerous() && !finalRsWorldSelector.isPvp()) return false;
                        if (entry.isSkillTotal()) return false;

                        return true;
                    })
                    .sorted((o1, o2) -> Double.compare(o1.getPopulation(), o2.getPopulation()))
                    .collect(Collectors.toList());

            logger.trace("Current world population vs viable world list. {}, {}", currentWorldBotPopulation, betterWorlds);

            if (betterWorlds.size() > 0){
                WorldData world = betterWorlds.get(ThreadLocalRandom.current().nextInt(0, Math.min(5, betterWorlds.size())));
                logger.info("Found better world. betterWorld={}, betterWorldBotPop={}, currentWorld={}, currentWorldBotPop={}", world.getWorld(), world.getBotPopulation(), currentWorld, currentWorldBotPopulation);
                botControl.hopToWorld(world.getWorld());
                lastCheck = LocalDateTime.now().plusMinutes(1);
                return true;
            }

            lastCheck = LocalDateTime.now();
        }
        return false;
    }

}
