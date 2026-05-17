package com.arnav.lootjournal.output;

import com.arnav.lootjournal.LootJournalConfig;
import com.arnav.lootjournal.session.SessionReport;
import com.arnav.lootjournal.statistics.AggregateStatistics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Environment(EnvType.CLIENT)
public final class JsonExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LootJournal-IO");
        t.setDaemon(true);
        return t;
    });

    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private JsonExporter() {}

    public static void export(SessionReport report) {
        // Deep-copy the report to avoid mutation on the game thread after export
        String json = GSON.toJson(report);
        EXECUTOR.submit(() -> {
            if (LootJournalConfig.writeSessionJson) writeSession(json, report.startTime);
            if (LootJournalConfig.writeStatisticsJson) updateStatistics(report);
        });
    }

    private static void writeSession(String json, String startTime) {
        try {
            Path dir = baseDir().resolve("sessions");
            Files.createDirectories(dir);
            String filename = "session-" + formatTimestamp(startTime) + ".json";
            Path file = dir.resolve(filename);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write(json);
            }
        } catch (IOException e) {
            System.err.println("[LootJournal] Failed to write session file: " + e.getMessage());
        }
    }

    private static void updateStatistics(SessionReport report) {
        try {
            Path statsFile = baseDir().resolve("statistics.json");
            Files.createDirectories(baseDir());

            AggregateStatistics stats;
            if (Files.exists(statsFile)) {
                try (Reader r = Files.newBufferedReader(statsFile, StandardCharsets.UTF_8)) {
                    stats = GSON.fromJson(r, AggregateStatistics.class);
                    if (stats == null) stats = new AggregateStatistics();
                }
            } else {
                stats = new AggregateStatistics();
            }

            stats.totalSessions++;
            stats.lastUpdated = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());

            for (Map.Entry<String, SessionReport.BlockTypeAggregate> entry : report.blockAggregates.entrySet()) {
                String blockId = entry.getKey();
                SessionReport.BlockTypeAggregate sessionAgg = entry.getValue();

                AggregateStatistics.BlockStat stat =
                        stats.blockStatistics.computeIfAbsent(blockId, k -> new AggregateStatistics.BlockStat());
                stat.totalBroken += sessionAgg.totalBroken;

                for (Map.Entry<String, Integer> drop : sessionAgg.attributedDrops.entrySet()) {
                    String itemId = drop.getKey();
                    int count = drop.getValue();
                    AggregateStatistics.DropStat ds =
                            stat.dropFrequencies.computeIfAbsent(itemId, k -> new AggregateStatistics.DropStat());
                    ds.count += count;
                    ds.frequency = stat.totalBroken > 0
                            ? (double) ds.count / stat.totalBroken
                            : 0.0;
                }
            }

            try (Writer w = Files.newBufferedWriter(statsFile, StandardCharsets.UTF_8)) {
                w.write(GSON.toJson(stats));
            }
        } catch (IOException e) {
            System.err.println("[LootJournal] Failed to update statistics: " + e.getMessage());
        }
    }

    private static Path baseDir() {
        return FabricLoader.getInstance().getGameDir().resolve("loot-journal");
    }

    private static String formatTimestamp(String isoTime) {
        try {
            LocalDateTime dt = LocalDateTime.parse(isoTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return FILE_FMT.format(dt);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
