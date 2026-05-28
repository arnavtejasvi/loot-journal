package com.arnav.lootjournal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class LootJournalKeys {
    static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("lootjournal", "key"));

    public static KeyMapping showReport;
    public static KeyMapping openSettings;

    private LootJournalKeys() {}

    public static void register() {
        showReport = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "lootjournal.key.show_report",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));
        openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "lootjournal.key.open_settings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));
    }
}
