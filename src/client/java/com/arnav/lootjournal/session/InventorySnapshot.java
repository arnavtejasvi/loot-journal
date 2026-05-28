package com.arnav.lootjournal.session;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public record InventorySnapshot(Map<String, Integer> items, int totalXp) {

    public static InventorySnapshot of(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : player.getInventory()) {
            if (stack.isEmpty()) continue;
            String key = stack.getItem().getDescriptionId();
            counts.merge(key, stack.getCount(), Integer::sum);
        }
        return new InventorySnapshot(counts, player.totalExperience);
    }

    public static Map<String, Integer> diffGained(InventorySnapshot before, InventorySnapshot after) {
        Map<String, Integer> gained = new HashMap<>();
        for (Map.Entry<String, Integer> entry : after.items().entrySet()) {
            int beforeCount = before.items().getOrDefault(entry.getKey(), 0);
            int delta = entry.getValue() - beforeCount;
            if (delta > 0) gained.put(entry.getKey(), delta);
        }
        return gained;
    }

    public static Map<String, Integer> diffLost(InventorySnapshot before, InventorySnapshot after) {
        Map<String, Integer> lost = new HashMap<>();
        for (Map.Entry<String, Integer> entry : before.items().entrySet()) {
            int afterCount = after.items().getOrDefault(entry.getKey(), 0);
            int delta = entry.getValue() - afterCount;
            if (delta > 0) lost.put(entry.getKey(), delta);
        }
        return lost;
    }
}
