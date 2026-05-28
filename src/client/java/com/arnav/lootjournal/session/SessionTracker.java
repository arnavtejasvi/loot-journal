package com.arnav.lootjournal.session;

import com.arnav.lootjournal.LootJournalConfig;
import com.arnav.lootjournal.output.JsonExporter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class SessionTracker {
    private static SessionReport currentSession = null;
    private static InventorySnapshot lastSnapshot = null;
    private static AttributionWindow attributionWindow = null;
    private static int sessionStartXp = -1;

    private SessionTracker() {}

    public static void start(Minecraft client, String worldName) {
        currentSession = new SessionReport(worldName, currentIsoTime());
        attributionWindow = new AttributionWindow();
        lastSnapshot = null;
        sessionStartXp = -1;
    }

    public static void tick(Minecraft client) {
        if (!LootJournalConfig.enabled || currentSession == null || client.player == null) return;

        InventorySnapshot snap = InventorySnapshot.of(client.player);

        if (lastSnapshot == null) {
            lastSnapshot = snap;
            sessionStartXp = snap.totalXp();
            return;
        }

        long tick = client.level != null ? client.level.getGameTime() : 0L;

        // Flush expired breaks first — gives us newly committed events for BR feedback.
        java.util.List<BlockBreakEvent> newCommits = attributionWindow.flushExpiredAndGetNewCommits(tick);

        Map<String, Integer> gained = InventorySnapshot.diffGained(lastSnapshot, snap);
        Map<String, Integer> lost = InventorySnapshot.diffLost(lastSnapshot, snap);

        if (!gained.isEmpty()) {
            Map<String, Integer> unattributed = attributionWindow.attributeGains(gained, tick);
            currentSession.mergeGained(gained);
            if (!unattributed.isEmpty()) {
                currentSession.addUnattributed(unattributed);
            }
        }

        if (!lost.isEmpty()) {
            currentSession.mergeLost(lost);
        }

        if (LootJournalConfig.blockRandomizerMode && LootJournalConfig.brFeedbackInChat) {
            for (BlockBreakEvent e : newCommits) {
                showBreakFeedback(client, e);
            }
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

    public static void end(Minecraft client) {
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
                if (LootJournalConfig.writeBreakEvents) {
                    currentSession.breakEvents.add(e);
                }
            }
        }

        currentSession.endTime = currentIsoTime();
        if (currentSession.startTime != null && currentSession.endTime != null) {
            try {
                LocalDateTime start = LocalDateTime.parse(currentSession.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                LocalDateTime end = LocalDateTime.parse(currentSession.endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                currentSession.durationMinutes = java.time.Duration.between(start, end).toMinutes();
            } catch (Exception ignored) {}
        }

        if (lastSnapshot != null && sessionStartXp >= 0) {
            currentSession.xpGained = lastSnapshot.totalXp() - sessionStartXp;
        }

        if (LootJournalConfig.showSummaryOnDisconnect && client.player != null) {
            printSummary(client);
        }

        if (LootJournalConfig.writeSessionJson || LootJournalConfig.writeStatisticsJson) {
            JsonExporter.export(currentSession);
        }

        currentSession = null;
        lastSnapshot = null;
        attributionWindow = null;
    }

    public static void printMidSessionReport(Minecraft client) {
        if (currentSession == null || client.player == null) return;
        printSummary(client);
    }

    private static void showBreakFeedback(Minecraft client, BlockBreakEvent event) {
        if (client.gui == null) return;
        String blockName = event.blockId.contains(":")
                ? event.blockId.substring(event.blockId.lastIndexOf(':') + 1).replace('_', ' ')
                : event.blockId;
        String drops = event.drops.isEmpty() ? "§7nothing" : "§a" + formatItemMap(event.drops);
        client.gui.getChat().addClientSystemMessage(
                Component.literal("§e[Randomizer] §f" + blockName + " §8→ " + drops));
    }

    private static void printSummary(Minecraft client) {
        if (client.gui == null) return;
        client.gui.getChat().addClientSystemMessage(
                Component.literal("§6=== Loot Journal Session Summary ==="));
        client.gui.getChat().addClientSystemMessage(
                Component.literal("§aGained: " + formatItemMap(currentSession.totalGained)));
        client.gui.getChat().addClientSystemMessage(
                Component.literal("§cLost: " + formatItemMap(currentSession.totalLost)));
        client.gui.getChat().addClientSystemMessage(
                Component.literal("§eXP gained: +" + currentSession.xpGained));
        client.gui.getChat().addClientSystemMessage(
                Component.literal("§7Blocks broken: " + currentSession.blockAggregates.size() + " type(s)"));
        if (LootJournalConfig.writeSessionJson) {
            client.gui.getChat().addClientSystemMessage(
                    Component.literal("§7Session saved to loot-journal/sessions/"));
        }
    }

    private static String formatItemMap(Map<String, Integer> map) {
        if (map.isEmpty()) return "nothing";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (i > 0) sb.append(", ");
            String name = e.getKey().contains(":") ? e.getKey().substring(e.getKey().lastIndexOf(':') + 1) : e.getKey();
            sb.append(name.replace('_', ' ')).append(" x").append(e.getValue());
            if (++i >= 5 && map.size() > 5) {
                sb.append(" (+").append(map.size() - 5).append(" more)");
                break;
            }
        }
        return sb.toString();
    }

    private static String currentIsoTime() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    }

    public static boolean isActive() {
        return currentSession != null;
    }
}
