package com.arnav.lootjournal;

import com.arnav.lootjournal.output.JsonExporter;
import com.arnav.lootjournal.session.AttributionWindow;
import com.arnav.lootjournal.session.BlockBreakEvent;
import com.arnav.lootjournal.session.InventorySnapshot;
import com.arnav.lootjournal.session.SessionReport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class LootJournalSession {
    private static SessionReport     currentSession    = null;
    private static InventorySnapshot lastSnapshot      = null;
    private static AttributionWindow attributionWindow = null;
    private static int               sessionStartXp    = -1;

    private LootJournalSession() {}

    public static void start(String worldName) {
        currentSession    = new SessionReport(worldName, currentIsoTime());
        attributionWindow = new AttributionWindow();
        lastSnapshot      = null;
        sessionStartXp    = -1;
    }

    public static void tick(Minecraft mc) {
        if (!LootJournalConfig.enabled || currentSession == null || mc.player == null) return;

        InventorySnapshot snap = InventorySnapshot.of(mc.player);
        if (lastSnapshot == null) {
            lastSnapshot   = snap;
            sessionStartXp = snap.totalXp();
            return;
        }

        long tick = mc.level != null ? mc.level.getGameTime() : 0L;

        List<BlockBreakEvent> newCommits = attributionWindow.flushExpiredAndGetNewCommits(tick);

        Map<String, Integer> gained = InventorySnapshot.diffGained(lastSnapshot, snap);
        Map<String, Integer> lost   = InventorySnapshot.diffLost(lastSnapshot, snap);

        if (!gained.isEmpty()) {
            Map<String, Integer> unattributed = attributionWindow.attributeGains(gained, tick);
            currentSession.mergeGained(gained);
            if (!unattributed.isEmpty()) currentSession.addUnattributed(unattributed);
        }
        if (!lost.isEmpty()) currentSession.mergeLost(lost);

        if (LootJournalConfig.blockRandomizerMode && LootJournalConfig.brFeedbackInChat) {
            for (BlockBreakEvent e : newCommits) showBreakFeedback(mc, e);
        }

        lastSnapshot = snap;
    }

    public static void onBlockBroken(String blockId, int x, int y, int z, long tick) {
        if (currentSession == null || !LootJournalConfig.enabled) return;
        attributionWindow.addBreak(new BlockBreakEvent(blockId, x, y, z, tick));
        currentSession.blockAggregates
                .computeIfAbsent(blockId, k -> new SessionReport.BlockTypeAggregate()).totalBroken++;
    }

    public static void onMobKilled(String entityTypeId) {
        if (currentSession == null || !LootJournalConfig.enabled) return;
        currentSession.mobKills.merge(entityTypeId, 1, Integer::sum);
    }

    public static void end(Minecraft mc) {
        if (currentSession == null) return;

        attributionWindow.flushAll();
        for (BlockBreakEvent e : attributionWindow.getCommitted()) {
            if (!e.drops.isEmpty()) {
                SessionReport.BlockTypeAggregate agg = currentSession.blockAggregates
                        .computeIfAbsent(e.blockId, k -> new SessionReport.BlockTypeAggregate());
                e.drops.forEach((item, count) -> agg.attributedDrops.merge(item, count, Integer::sum));
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

        if (LootJournalConfig.showSummaryOnDisconnect && mc.player != null) printSummary(mc, currentSession);
        if (LootJournalConfig.writeSessionJson || LootJournalConfig.writeStatisticsJson) {
            JsonExporter.export(currentSession);
        }

        currentSession    = null;
        lastSnapshot      = null;
        attributionWindow = null;
    }

    public static void printMidSessionReport(Minecraft mc) {
        if (currentSession == null || mc.player == null) return;
        printSummary(mc, currentSession);
    }

    private static void showBreakFeedback(Minecraft mc, BlockBreakEvent event) {
        if (mc.gui == null) return;
        String blockName = event.blockId.contains(":")
                ? event.blockId.substring(event.blockId.lastIndexOf(':') + 1).replace('_', ' ')
                : event.blockId;
        String drops = event.drops.isEmpty() ? "§7nothing" : "§a" + formatItemMap(event.drops);
        mc.gui.getChat().addMessage(Component.literal("§e[Randomizer] §f" + blockName + " §8→ " + drops));
    }

    private static void printSummary(Minecraft mc, SessionReport session) {
        if (mc.gui == null) return;

        String elapsed = formatElapsed(session.startTime);

        mc.gui.getChat().addMessage(Component.literal("§6=== Loot Journal Session Summary ==="));
        mc.gui.getChat().addMessage(Component.literal("§7Session: §f" + session.worldName + " §8(" + elapsed + ")"));
        mc.gui.getChat().addMessage(Component.literal("§aGained: " + formatItemMap(session.totalGained)));
        mc.gui.getChat().addMessage(Component.literal("§cLost: "   + formatItemMap(session.totalLost)));
        mc.gui.getChat().addMessage(Component.literal("§eXP gained: +" + session.xpGained));
        if (!session.mobKills.isEmpty()) {
            mc.gui.getChat().addMessage(Component.literal("§bMobs killed: " + formatMobKills(session.mobKills)));
        }
        mc.gui.getChat().addMessage(Component.literal("§7Blocks broken: " + session.blockAggregates.size() + " type(s)"));
        if (LootJournalConfig.writeSessionJson) {
            mc.gui.getChat().addMessage(Component.literal("§7Session saved to loot-journal/sessions/"));
        }
    }

    private static String formatElapsed(String startTimeIso) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTimeIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long secs = Duration.between(start, LocalDateTime.now()).getSeconds();
            long h = secs / 3600;
            long m = (secs % 3600) / 60;
            long s = secs % 60;
            if (h > 0) return h + "h " + m + "m";
            if (m > 0) return m + "m " + s + "s";
            return s + "s";
        } catch (Exception e) {
            return "?";
        }
    }

    static String formatItemMap(Map<String, Integer> map) {
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
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String currentIsoTime() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    }

    public static boolean isActive() {
        return currentSession != null;
    }
}
