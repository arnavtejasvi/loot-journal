package com.arnav.lootjournal.session;

import com.arnav.lootjournal.LootJournalConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributionWindow {
    private static final int MAX_PENDING_NORMAL = 10;
    private static final int MAX_PENDING_BR     = 1;

    private final ArrayDeque<BlockBreakEvent> pending   = new ArrayDeque<>();
    private final List<BlockBreakEvent>       committed = new ArrayList<>();

    private BlockBreakEvent lastCommitted     = null;
    private long            lastCommittedTick = -1L;

    public void addBreak(BlockBreakEvent event) {
        int max = LootJournalConfig.blockRandomizerMode ? MAX_PENDING_BR : MAX_PENDING_NORMAL;
        while (pending.size() >= max) commitFront(event.tickTime);
        pending.addLast(event);
    }

    public List<BlockBreakEvent> flushExpiredAndGetNewCommits(long currentTick) {
        List<BlockBreakEvent> newCommits = new ArrayList<>();
        int window = LootJournalConfig.attributionWindowTicks;
        while (!pending.isEmpty() && currentTick - pending.peekFirst().tickTime > window) {
            newCommits.add(commitFront(currentTick));
        }
        return newCommits.isEmpty() ? Collections.emptyList() : newCommits;
    }

    public Map<String, Integer> attributeGains(Map<String, Integer> gained, long currentTick) {
        if (!pending.isEmpty()) {
            pending.peekFirst().addDrops(gained);
            return Map.of();
        }
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
        while (!pending.isEmpty()) commitFront(-1L);
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
