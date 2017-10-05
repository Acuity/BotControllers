package com.acuity.control.client.managers.world;

import com.acuity.common.world_data_parser.WorldData;
import com.acuity.common.world_data_parser.WorldDataResult;
import com.acuity.control.client.BotControl;
import com.acuity.db.domain.common.RSWorldSelector;
import com.acuity.db.domain.vertex.impl.rs_account.RSAccount;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptNode;
import com.acuity.db.domain.vertex.impl.scripts.selector.ScriptSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 9/25/2017.
 */
public class WorldManager {

    private static final Logger logger = LoggerFactory.getLogger(WorldManager.class);

    private BotControl botControl;
    private LocalDateTime lastCheck = LocalDateTime.MIN;

    private int acceptablePopulationDifference = 2;
    private boolean filterNonMatchingTags = false;

    public WorldManager(BotControl botControl) {
        this.botControl = botControl;
    }

    public int getAcceptablePopulationDifference() {
        return acceptablePopulationDifference;
    }

    public boolean isFilterNonMatchingTags() {
        return filterNonMatchingTags;
    }

    public WorldManager setAcceptablePopulationDifference(int acceptablePopulationDifference) {
        this.acceptablePopulationDifference = acceptablePopulationDifference;
        return this;
    }

    public WorldManager setFilterNonMatchingTags(boolean filterNonMatchingTags) {
        this.filterNonMatchingTags = filterNonMatchingTags;
        return this;
    }

    public boolean onLoop(){
        if (!botControl.getClientManager().isSignedIn()) return false;

        RSWorldSelector rsWorldSelector = Optional.ofNullable(botControl.getBotClientConfig().getScriptSelector()).map(ScriptSelector::getRsWorldSelector).orElse(null);
        rsWorldSelector = botControl.getScriptManager().getExecutionNode().map(ScriptNode::getRsWorldSelector).orElse(rsWorldSelector);

        if (rsWorldSelector != null && !rsWorldSelector.isEnabled()) return false;

        Integer currentWorld = botControl.getClientManager().getCurrentWorld();
        if (currentWorld == null) return false;

        if (lastCheck.isBefore(LocalDateTime.now().minusSeconds(10))){
            WorldDataResult worldData = botControl.getRemote().requestWorldData();
            if (worldData == null) return false;

            if (filterNonMatchingTags){
                Set<String> tagIDs = Optional.ofNullable(botControl.getRsAccountManager().getRsAccount()).map(RSAccount::getTagIDs).orElse(Collections.emptySet());
                worldData.zip(otherTagIDs -> tagIDs.stream().anyMatch(otherTagIDs::contains));
            }
            else {
                worldData.zip();
            }

            int currentWorldBotPopulation = worldData.getWorldBotPopulation().getOrDefault(currentWorld, Integer.MAX_VALUE) - acceptablePopulationDifference;

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
                    .sorted(Comparator.comparingDouble(WorldData::getPopulation))
                    .collect(Collectors.toList());

            logger.trace("Current world population vs viable world list. {}, {}", currentWorldBotPopulation, betterWorlds);

            if (betterWorlds.size() > 0){
                WorldData world = betterWorlds.get(ThreadLocalRandom.current().nextInt(0, Math.min(5, betterWorlds.size())));
                logger.info("Found better world. betterWorld={}, betterWorldBotPop={}, currentWorld={}, currentWorldBotPop={}", world.getWorld(), world.getBotPopulation(), currentWorld, currentWorldBotPopulation);
                botControl.getClientManager().hopToWorld(world.getWorld());
                lastCheck = LocalDateTime.now().plusMinutes(1);
                return true;
            }

            lastCheck = LocalDateTime.now();
        }
        return false;
    }

}
