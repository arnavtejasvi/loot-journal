package com.arnav.lootjournal.session;

import com.arnav.lootjournal.LootJournalConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionReport {
    public String worldName;
    public String startTime;
    public String endTime;
    public long durationSeconds;
    public long durationMinutes;

    public Map<String, Integer> totalGained      = new HashMap<>();
    public Map<String, Integer> totalLost         = new HashMap<>();
    public Map<String, Integer> unattributedGains = new HashMap<>();
    public Map<String, Integer> mobKills          = new ConcurrentHashMap<>();
    public int xpGained;

    public Map<String, BlockTypeAggregate> blockAggregates = new HashMap<>();
    public List<BlockBreakEvent>           breakEvents     = new ArrayList<>();

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
        if (LootJournalConfig.writeBreakEvents) breakEvents.add(event);
    }

    public void mergeGained(Map<String, Integer> gained) {
        gained.forEach((k, v) -> totalGained.merge(k, v, Integer::sum));
    }

    public void mergeLost(Map<String, Integer> lost) {
        lost.forEach((k, v) -> totalLost.merge(k, v, Integer::sum));
    }

    public void addUnattributed(Map<String, Integer> items) {
        items.forEach((k, v) -> unattributedGains.merge(k, v, Integer::sum));
    }

    public static class BlockTypeAggregate {
        public int totalBroken = 0;
        public Map<String, Integer> attributedDrops = new HashMap<>();
    }
}
