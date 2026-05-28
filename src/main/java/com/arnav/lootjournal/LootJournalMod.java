package com.arnav.lootjournal;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@Mod("lootjournal")
public class LootJournalMod {

    public LootJournalMod(IEventBus modEventBus) {
        LootJournalConfig.load();
        modEventBus.addListener(this::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LootJournalKeys.onRegisterKeyMappings(event);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        LootJournalSession.tick(mc);

        while (LootJournalKeys.SHOW_REPORT.consumeClick()) {
            LootJournalSession.printMidSessionReport(mc);
        }
        while (LootJournalKeys.OPEN_SETTINGS.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new LootJournalConfigScreen(null));
            }
        }
        while (LootJournalKeys.OPEN_HISTORY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new SessionHistoryScreen(null));
            }
        }
    }

    private void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        String worldName = resolveWorldName(mc);
        LootJournalSession.start(worldName);
    }

    private void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LootJournalSession.end(Minecraft.getInstance());
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        String entityTypeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(event.getEntity().getType()).toString();
        LootJournalSession.onMobKilled(entityTypeId);
    }

    private static String resolveWorldName(Minecraft mc) {
        if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip;
        if (mc.getSingleplayerServer() != null) return mc.getSingleplayerServer().getWorldData().getLevelName();
        return "unknown";
    }
}
