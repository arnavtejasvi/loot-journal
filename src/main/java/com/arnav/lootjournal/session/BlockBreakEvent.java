package com.arnav.lootjournal.session;

import java.util.HashMap;
import java.util.Map;

public class BlockBreakEvent {
    public final String blockId;
    public final int x, y, z;
    public final long tickTime;
    public final Map<String, Integer> drops;

    public BlockBreakEvent(String blockId, int x, int y, int z, long tickTime) {
        this.blockId = blockId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tickTime = tickTime;
        this.drops = new HashMap<>();
    }

    public void addDrops(Map<String, Integer> gained) {
        for (Map.Entry<String, Integer> entry : gained.entrySet()) {
            drops.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }
}
