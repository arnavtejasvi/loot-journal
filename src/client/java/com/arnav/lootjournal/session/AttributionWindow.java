package com.arnav.lootjournal.session;

import com.arnav.lootjournal.LootJournalConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class AttributionWindow {
    // Normal mode: queue up to 10 concurrent pending breaks (rapid mining).
    // BR mode: queue only 1 — one break at a time, wait for drops before the next.
    private static final int MAX_PENDING_NORMAL = 10;
    private static final int MAX_PENDING_BR     = 1;

    private final ArrayDeque<BlockBreakEvent> pending   = new ArrayDeque<>();
    private final List<BlockBreakEvent>       committed = new ArrayList<>();

    // BR failsafe: if the window expired but items arrive late (server lag spike),
    // attribute them to the last committed break within 2x the window.
    private BlockBreakEvent lastCommitted     = null;
    private long            lastCommittedTick = -1L;

    public void addBreak(BlockBreakEvent event) {
        int max = LootJournalConfig.blockRandomizerMode ? MAX_PENDING_BR : MAX_PENDING_NORMAL;
        while (pending.size() >= max) {
            commitFront(event.tickTime);
        }
        pending.addLast(event);
    }

    // Flushes expired breaks and returns them — caller uses this list for BR chat feedback.
    // Must be called BEFORE attributeGains each tick so the pending queue is current.
    public List<BlockBreakEvent> flushExpiredAndGetNewCommits(long currentTick) {
        List<BlockBreakEvent> newCommits = new ArrayList<>();
        int window = LootJournalConfig.attributionWindowTicks;
        while (!pending.isEmpty() && currentTick - pending.peekFirst().tickTime > window) {
            newCommits.add(commitFront(currentTick));
        }
        return newCommits.isEmpty() ? Collections.emptyList() : newCommits;
    }

    // Attributes gained items to the oldest pending break.
    // Caller must call flushExpiredAndGetNewCommits first to keep the queue fresh.
    // Returns any gains that could not be attributed (caller puts in unattributedGains).
    public Map<String, Integer> attributeGains(Map<String, Integer> gained, long currentTick) {
        if (!pending.isEmpty()) {
            pending.peekFirst().addDrops(gained);
            return Map.of();
        }

        // BR failsafe: items arrived after the window but within 2x tolerance.
        // This covers server lag spikes where the loot table lookup took extra time.
        if (LootJournalConfig.blockRandomizerMode && lastCommitted != null) {
            long tolerance = (long) LootJournalConfig.attributionWindowTicks * 2;
            if (currentTick - lastCommittedTick <= tolerance) {
                lastCommitted.addDrops(gained);
                return Map.of();
            }
        }

        return new HashMap<>(gained);
    }

    public void flushAll() {
        while (!pending.isEmpty()) {
            commitFront(-1L);
        }
    }

    public List<BlockBreakEvent> getCommitted() {
        return committed;
    }

    private BlockBreakEvent commitFront(long atTick) {
        BlockBreakEvent e = pending.pollFirst();
        committed.add(e);
        lastCommitted     = e;
        lastCommittedTick = atTick;
        return e;
    }
}
