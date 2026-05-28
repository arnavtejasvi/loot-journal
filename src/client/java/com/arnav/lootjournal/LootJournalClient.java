package com.arnav.lootjournal;

import com.arnav.lootjournal.session.SessionTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class LootJournalClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LootJournalConfig.load();
        LootJournalKeys.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String worldName = resolveWorldName(client);
            SessionTracker.start(client, worldName);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SessionTracker.end(client);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SessionTracker.tick(client);

            if (LootJournalKeys.showReport != null) {
                while (LootJournalKeys.showReport.consumeClick()) {
                    SessionTracker.printMidSessionReport(client);
                }
            }

            if (LootJournalKeys.openSettings != null && client.screen == null) {
                while (LootJournalKeys.openSettings.consumeClick()) {
                    client.setScreen(new LootJournalConfigScreen(null));
                }
            }
        });
    }

    private String resolveWorldName(Minecraft client) {
        if (client.getCurrentServer() != null) {
            return client.getCurrentServer().ip;
        }
        if (client.getSingleplayerServer() != null) {
            return "singleplayer";
        }
        return "unknown";
    }
}
