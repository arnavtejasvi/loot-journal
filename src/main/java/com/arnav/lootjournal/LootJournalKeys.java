package com.arnav.lootjournal;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class LootJournalKeys {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("lootjournal", "general"));

    public static final KeyMapping SHOW_REPORT = new KeyMapping(
        "lootjournal.key.show_report",
        GLFW.GLFW_KEY_J,
        CATEGORY
    );

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
        "lootjournal.key.open_settings",
        GLFW.GLFW_KEY_UNKNOWN,
        CATEGORY
    );

    public static final KeyMapping OPEN_HISTORY = new KeyMapping(
        "lootjournal.key.open_history",
        GLFW.GLFW_KEY_UNKNOWN,
        CATEGORY
    );

    private LootJournalKeys() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(SHOW_REPORT);
        event.register(OPEN_SETTINGS);
        event.register(OPEN_HISTORY);
    }
}
