package com.arnav.lootjournal.output;

import com.arnav.lootjournal.LootJournalConfig;
import com.arnav.lootjournal.session.SessionReport;
import com.arnav.lootjournal.statistics.AggregateStatistics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public final class JsonExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LootJournal-IO");
        t.setDaemon(true);
        return t;
    });
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private JsonExporter() {}

    public static void export(SessionReport report) {
        String json = GSON.toJson(report);
        EXECUTOR.submit(() -> {
            if (LootJournalConfig.writeSessionJson)    writeSession(json, report.startTime);
            if (LootJournalConfig.writeStatisticsJson) updateStatistics(report);
        });
    }

    private static void writeSession(String json, String startTime) {
        try {
            Path dir = baseDir().resolve("sessions");
            Files.createDirectories(dir);
            Path file = dir.resolve("session-" + formatTimestamp(startTime) + ".json");
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
                    AggregateStatistics.DropStat ds =
                            stat.dropFrequencies.computeIfAbsent(drop.getKey(), k -> new AggregateStatistics.DropStat());
                    ds.count += drop.getValue();
                    ds.frequency = stat.totalBroken > 0 ? (double) ds.count / stat.totalBroken : 0.0;
                }
            }

            try (Writer w = Files.newBufferedWriter(statsFile, StandardCharsets.UTF_8)) {
                w.write(GSON.toJson(stats));
            }
        } catch (IOException e) {
            System.err.println("[LootJournal] Failed to update statistics: " + e.getMessage());
        }
    }

    public static List<SessionReport> loadRecentSessions(int max) {
        List<SessionReport> results = new ArrayList<>();
        Path dir = baseDir().resolve("sessions");
        if (!Files.isDirectory(dir)) return results;
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                 .sorted(Comparator.reverseOrder())
                 .limit(max)
                 .forEach(p -> {
                     try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                         SessionReport report = GSON.fromJson(r, SessionReport.class);
                         if (report != null) results.add(report);
                     } catch (Exception ignored) {}
                 });
        } catch (IOException e) {
            System.err.println("[LootJournal] Failed to list sessions: " + e.getMessage());
        }
        return results;
    }

    private static Path baseDir() {
        return FMLPaths.GAMEDIR.get().resolve("loot-journal");
    }

    private static String formatTimestamp(String isoTime) {
        try {
            return FILE_FMT.format(LocalDateTime.parse(isoTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
            return "unknown";
        }
    }
}
