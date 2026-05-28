package com.arnav.lootjournal.session;

import com.arnav.lootjournal.LootJournalConfig;
import com.arnav.lootjournal.output.JsonExporter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class SessionTracker {
    private static SessionReport currentSession = null;
    private static InventorySnapshot lastSnapshot = null;
    private static AttributionWindow attributionWindow = null;
    private static int sessionStartXp = -1;

    private SessionTracker() {}

    public static void start(MinecraftClient client, String worldName) {
        currentSession = new SessionReport(worldName, currentIsoTime());
        attributionWindow = new AttributionWindow();
        lastSnapshot = null;
        sessionStartXp = -1;
    }

    public static void tick(MinecraftClient client) {
        if (!LootJournalConfig.enabled || currentSession == null || client.player == null) return;

        InventorySnapshot snap = InventorySnapshot.of(client.player);

        if (lastSnapshot == null) {
            lastSnapshot = snap;
            sessionStartXp = snap.totalXp();
            return;
        }

        long tick = client.world != null ? client.world.getTime() : 0L;

        List<BlockBreakEvent> newCommits = attributionWindow.flushExpiredAndGetNewCommits(tick);

        Map<String, Integer> gained = InventorySnapshot.diffGained(lastSnapshot, snap);
        Map<String, Integer> lost = InventorySnapshot.diffLost(lastSnapshot, snap);

        if (!gained.isEmpty()) {
            Map<String, Integer> unattributed = attributionWindow.attributeGains(gained, tick);
            currentSession.mergeGained(gained);
            if (!unattributed.isEmpty()) currentSession.addUnattributed(unattributed);
        }
        if (!lost.isEmpty()) currentSession.mergeLost(lost);

        if (LootJournalConfig.blockRandomizerMode && LootJournalConfig.brFeedbackInChat) {
            for (BlockBreakEvent e : newCommits) showBreakFeedback(client, e);
        }

        lastSnapshot = snap;
    }

    public static void onBlockBroken(String blockId, int x, int y, int z, long tick) {
        if (currentSession == null || !LootJournalConfig.enabled) return;
        BlockBreakEvent event = new BlockBreakEvent(blockId, x, y, z, tick);
        attributionWindow.addBreak(event);
        currentSession.blockAggregates.computeIfAbsent(blockId,
                k -> new SessionReport.BlockTypeAggregate()).totalBroken++;
    }

    public static void onMobKilled(String entityTypeId) {
        if (currentSession == null || !LootJournalConfig.enabled) return;
        currentSession.mobKills.merge(entityTypeId, 1, Integer::sum);
    }

    public static void end(MinecraftClient client) {
        if (currentSession == null) return;

        attributionWindow.flushAll();
        for (BlockBreakEvent e : attributionWindow.getCommitted()) {
            if (!e.drops.isEmpty()) {
                SessionReport.BlockTypeAggregate agg =
                        currentSession.blockAggregates.computeIfAbsent(e.blockId,
                                k -> new SessionReport.BlockTypeAggregate());
                for (Map.Entry<String, Integer> drop : e.drops.entrySet()) {
                    agg.attributedDrops.merge(drop.getKey(), drop.getValue(), Integer::sum);
                }
                if (LootJournalConfig.writeBreakEvents) currentSession.breakEvents.add(e);
            }
        }

        currentSession.endTime = currentIsoTime();
        try {
            LocalDateTime start = LocalDateTime.parse(currentSession.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime end   = LocalDateTime.parse(currentSession.endTime,   DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            currentSession.durationSeconds = Duration.between(start, end).getSeconds();
            currentSession.durationMinutes = currentSession.durationSeconds / 60;
        } catch (Exception ignored) {}

        if (lastSnapshot != null && sessionStartXp >= 0) {
            currentSession.xpGained = lastSnapshot.totalXp() - sessionStartXp;
        }

        if (LootJournalConfig.showSummaryOnDisconnect && client.player != null) {
            printSummary(client, currentSession);
        }
        if (LootJournalConfig.writeSessionJson || LootJournalConfig.writeStatisticsJson) {
            JsonExporter.export(currentSession);
        }

        currentSession = null;
        lastSnapshot = null;
        attributionWindow = null;
    }

    public static void printMidSessionReport(MinecraftClient client) {
        if (currentSession == null || client.player == null) return;
        printSummary(client, currentSession);
    }

    private static void showBreakFeedback(MinecraftClient client, BlockBreakEvent event) {
        if (client.inGameHud == null) return;
        String blockName = event.blockId.contains(":")
                ? event.blockId.substring(event.blockId.lastIndexOf(':') + 1).replace('_', ' ')
                : event.blockId;
        String drops = event.drops.isEmpty() ? "§7nothing" : "§a" + formatItemMap(event.drops);
        client.inGameHud.getChatHud().addMessage(
                Text.literal("§e[Randomizer] §f" + blockName + " §8→ " + drops));
    }

    private static void printSummary(MinecraftClient client, SessionReport session) {
        if (client.inGameHud == null) return;
        String elapsed = formatElapsed(session.startTime);
        client.inGameHud.getChatHud().addMessage(Text.literal("§6=== Loot Journal Session Summary ==="));
        client.inGameHud.getChatHud().addMessage(Text.literal("§7Session: §f" + session.worldName + " §8(" + elapsed + ")"));
        client.inGameHud.getChatHud().addMessage(Text.literal("§aGained: " + formatItemMap(session.totalGained)));
        client.inGameHud.getChatHud().addMessage(Text.literal("§cLost: " + formatItemMap(session.totalLost)));
        client.inGameHud.getChatHud().addMessage(Text.literal("§eXP gained: +" + session.xpGained));
        if (!session.mobKills.isEmpty()) {
            client.inGameHud.getChatHud().addMessage(Text.literal("§bMobs killed: " + formatMobKills(session.mobKills)));
        }
        client.inGameHud.getChatHud().addMessage(Text.literal("§7Blocks broken: " + session.blockAggregates.size() + " type(s)"));
        if (LootJournalConfig.writeSessionJson) {
            client.inGameHud.getChatHud().addMessage(Text.literal("§7Session saved to loot-journal/sessions/"));
        }
    }

    private static String formatElapsed(String startTimeIso) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTimeIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long secs = Duration.between(start, LocalDateTime.now()).getSeconds();
            long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
            if (h > 0) return h + "h " + m + "m";
            if (m > 0) return m + "m " + s + "s";
            return s + "s";
        } catch (Exception e) {
            return "?";
        }
    }

    public static String formatItemMap(Map<String, Integer> map) {
        if (map.isEmpty()) return "nothing";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (i > 0) sb.append(", ");
            String name = e.getKey().contains(":") ? e.getKey().substring(e.getKey().lastIndexOf(':') + 1) : e.getKey();
            sb.append(name.replace('_', ' ')).append(" x").append(e.getValue());
            if (++i >= 5 && map.size() > 5) { sb.append(" (+").append(map.size() - 5).append(" more)"); break; }
        }
        return sb.toString();
    }

    private static String formatMobKills(Map<String, Integer> kills) {
        if (kills.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        int total = kills.values().stream().mapToInt(Integer::intValue).sum();
        int i = 0;
        for (Map.Entry<String, Integer> e : kills.entrySet()) {
            if (i > 0) sb.append(", ");
            String name = e.getKey().contains(":") ? e.getKey().substring(e.getKey().lastIndexOf(':') + 1) : e.getKey();
            sb.append(capitalize(name.replace('_', ' '))).append(" x").append(e.getValue());
            if (++i >= 4 && kills.size() > 4) { sb.append(" (+").append(kills.size() - 4).append(" more)"); break; }
        }
        sb.append(" §8[").append(total).append(" total]");
        return sb.toString();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String currentIsoTime() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    }

    public static boolean isActive() {
        return currentSession != null;
    }
}
