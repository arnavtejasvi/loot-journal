package com.arnav.lootjournal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class LootJournalKeys {
    private static final KeyBinding.Category CATEGORY =
            new KeyBinding.Category(Identifier.of("lootjournal", "general"));

    public static KeyBinding showReport;
    public static KeyBinding openSettings;
    public static KeyBinding openHistory;

    private LootJournalKeys() {}

    public static void register() {
        showReport = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "lootjournal.key.show_report",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "lootjournal.key.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));
        openHistory = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "lootjournal.key.open_history",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));
    }
}
