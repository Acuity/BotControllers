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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 9/25/2017.
 */
public class WorldManager {

    private static final Logger logger = LoggerFactory.getLogger(WorldManager.class);

    private BotControl botControl;
    private Instant lastCheck = Instant.MIN;

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
        logger.info("Population difference set. {}", acceptablePopulationDifference);
        return this;
    }

    public WorldManager setFilterNonMatchingTags(boolean filterNonMatchingTags) {
        this.filterNonMatchingTags = filterNonMatchingTags;
        logger.info("Filter non matching tags set. {}", filterNonMatchingTags);
        return this;
    }

    public boolean onLoop(){
        if (!botControl.getClientInterface().isSignedIn()) return false;

        RSWorldSelector rsWorldSelector = Optional.ofNullable(botControl.getBotClientConfig().getScriptSelector()).map(ScriptSelector::getRsWorldSelector).orElse(null);
        rsWorldSelector = botControl.getScriptManager().getExecutionNode().map(ScriptNode::getRsWorldSelector).orElse(rsWorldSelector);

        if (rsWorldSelector != null && !rsWorldSelector.isEnabled()) return false;

        Integer currentWorld = botControl.getClientInterface().getCurrentWorld();
        if (currentWorld == null) return false;

        if (lastCheck.isBefore(Instant.now().minusSeconds(10))){
            WorldDataResult worldData = botControl.getRemote().requestWorldData();
            if (worldData == null) return false;

            if (filterNonMatchingTags){
                Set<String> tagIDs = Optional.ofNullable(botControl.getRsAccountManager().getRsAccount()).map(RSAccount::getTagIDs).orElse(Collections.emptySet());
                worldData.zip(otherTagIDs -> tagIDs.stream().anyMatch(otherTagIDs::contains));
            }
            else {
                worldData.zip();
            }

            int currentWorldBotPopulation = worldData.getWorldBotPopulation().getOrDefault(currentWorld, Integer.MAX_VALUE);

            RSWorldSelector finalRsWorldSelector = rsWorldSelector;

            List<WorldData> betterWorlds = worldData.getWorldData().stream()
                    .filter(entry -> entry.getBotPopulation() < (currentWorldBotPopulation - acceptablePopulationDifference))
                    .filter(entry -> finalRsWorldSelector == null || finalRsWorldSelector.getValidWorlds() == null || finalRsWorldSelector.getValidWorlds().contains(entry.getWorld()))
                    .sorted(Comparator.comparingDouble(WorldData::getPopulation))
                    .collect(Collectors.toList());

            logger.trace("Current world population vs viable world list. {}, {}", currentWorldBotPopulation, betterWorlds);

            if (betterWorlds.size() > 0){
                WorldData world = betterWorlds.get(ThreadLocalRandom.current().nextInt(0, Math.min(5, betterWorlds.size())));
                logger.info("Found better world. better: {}={}, current: {}={}", world.getWorld(), world.getBotPopulation(), currentWorld, currentWorldBotPopulation);
                botControl.getClientInterface().hopToWorld(world.getWorld());
                lastCheck = Instant.now().plus(1, ChronoUnit.MINUTES);
                return true;
            }

            lastCheck = Instant.now();
        }
        return false;
    }

}
