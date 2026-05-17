package com.arnav.lootjournal.session;

import com.arnav.lootjournal.LootJournalConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class SessionReport {
    public String worldName;
    public String startTime;
    public String endTime;
    public long durationMinutes;

    public Map<String, Integer> totalGained = new HashMap<>();
    public Map<String, Integer> totalLost = new HashMap<>();
    public Map<String, Integer> unattributedGains = new HashMap<>();
    public int xpGained;

    public Map<String, BlockTypeAggregate> blockAggregates = new HashMap<>();
    public List<BlockBreakEvent> breakEvents = new ArrayList<>();

    public SessionReport(String worldName, String startTime) {
        this.worldName = worldName;
        this.startTime = startTime;
    }

    public void recordBreak(BlockBreakEvent event) {
        BlockTypeAggregate agg = blockAggregates.computeIfAbsent(event.blockId, k -> new BlockTypeAggregate());
        agg.totalBroken++;
        for (Map.Entry<String, Integer> drop : event.drops.entrySet()) {
            agg.attributedDrops.merge(drop.getKey(), drop.getValue(), Integer::sum);
        }
        if (LootJournalConfig.writeBreakEvents) {
            breakEvents.add(event);
        }
    }

    public void incrementBreakCount(BlockBreakEvent event) {
        blockAggregates.computeIfAbsent(event.blockId, k -> new BlockTypeAggregate()).totalBroken++;
    }

    public void mergeGained(Map<String, Integer> gained) {
        for (Map.Entry<String, Integer> e : gained.entrySet()) {
            totalGained.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    public void mergeLost(Map<String, Integer> lost) {
        for (Map.Entry<String, Integer> e : lost.entrySet()) {
            totalLost.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    public void addUnattributed(Map<String, Integer> items) {
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            unattributedGains.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    public static class BlockTypeAggregate {
        public int totalBroken = 0;
        public Map<String, Integer> attributedDrops = new HashMap<>();
    }
}
