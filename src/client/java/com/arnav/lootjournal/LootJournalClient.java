package com.arnav.lootjournal;

import com.arnav.lootjournal.session.SessionTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

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

            if (LootJournalKeys.showReport != null && LootJournalKeys.showReport.wasPressed()) {
                SessionTracker.printMidSessionReport(client);
            }

            if (LootJournalKeys.openSettings != null && LootJournalKeys.openSettings.wasPressed()
                    && client.currentScreen == null) {
                client.setScreen(new LootJournalConfigScreen(null));
            }
        });
    }

    private String resolveWorldName(MinecraftClient client) {
        if (client.getCurrentServerEntry() != null) {
            return client.getCurrentServerEntry().address;
        }
        if (client.getServer() != null) {
            return client.getServer().getSaveProperties().getLevelName();
        }
        return "unknown";
    }
}
