package com.arnav.lootjournal.statistics;

import java.util.HashMap;
import java.util.Map;

public class AggregateStatistics {
    public String lastUpdated = "";
    public int totalSessions = 0;
    public Map<String, BlockStat> blockStatistics = new HashMap<>();

    public static class BlockStat {
        public int totalBroken = 0;
        public Map<String, DropStat> dropFrequencies = new HashMap<>();
    }

    public static class DropStat {
        public int count = 0;
        public double frequency = 0.0;

        public DropStat() {}
        public DropStat(int count, double frequency) {
            this.count = count;
            this.frequency = frequency;
        }
    }
}
