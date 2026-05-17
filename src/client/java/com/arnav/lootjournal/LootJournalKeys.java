package com.arnav.lootjournal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class LootJournalKeys {
    public static KeyBinding showReport;
    public static KeyBinding openSettings;

    private LootJournalKeys() {}

    public static void register() {
        showReport = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "lootjournal.key.show_report",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "lootjournal.key.category"
        ));
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "lootjournal.key.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default — user assigns in Controls
                "lootjournal.key.category"
        ));
    }
}
