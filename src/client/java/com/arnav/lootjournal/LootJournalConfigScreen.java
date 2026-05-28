package com.arnav.lootjournal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class LootJournalConfigScreen extends Screen {
    private final Screen parent;
    private int windowLabelY;

    public LootJournalConfigScreen(Screen parent) {
        super(Component.literal("Loot Journal Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx  = this.width / 2;
        int y   = 32;
        int gap = 24;

        addToggle(cx, y, "Loot Journal Enabled", LootJournalConfig.enabled,
                v -> LootJournalConfig.enabled = v);
        y += gap;

        this.addRenderableWidget(Button.builder(brModeText(), btn -> {
            LootJournalConfig.blockRandomizerMode = !LootJournalConfig.blockRandomizerMode;
            if (LootJournalConfig.blockRandomizerMode
                    && LootJournalConfig.attributionWindowTicks < 100) {
                LootJournalConfig.attributionWindowTicks = 100;
            }
            this.rebuildWidgets();
        }).pos(cx - 100, y).size(200, 20).build());
        y += gap;

        Button brFeedbackBtn = Button.builder(brFeedbackText(), btn -> {
            LootJournalConfig.brFeedbackInChat = !LootJournalConfig.brFeedbackInChat;
            btn.setMessage(brFeedbackText());
        }).pos(cx - 100, y).size(200, 20).build();
        brFeedbackBtn.active = LootJournalConfig.blockRandomizerMode;
        this.addRenderableWidget(brFeedbackBtn);
        y += gap;

        addToggle(cx, y, "Summary on Disconnect",
                LootJournalConfig.showSummaryOnDisconnect,
                v -> LootJournalConfig.showSummaryOnDisconnect = v);
        y += gap;

        addToggle(cx, y, "Write Session JSON",
                LootJournalConfig.writeSessionJson,
                v -> LootJournalConfig.writeSessionJson = v);
        y += gap;

        addToggle(cx, y, "Write Statistics JSON",
                LootJournalConfig.writeStatisticsJson,
                v -> LootJournalConfig.writeStatisticsJson = v);
        y += gap;

        addToggle(cx, y, "Write Per-Break Events",
                LootJournalConfig.writeBreakEvents,
                v -> LootJournalConfig.writeBreakEvents = v);
        y += gap;

        windowLabelY = y + 5;
        this.addRenderableWidget(Button.builder(Component.literal("  -  "), btn ->
                LootJournalConfig.attributionWindowTicks =
                        Math.max(20, LootJournalConfig.attributionWindowTicks - 20))
                .pos(cx - 100, y).size(40, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("  +  "), btn ->
                LootJournalConfig.attributionWindowTicks =
                        Math.min(200, LootJournalConfig.attributionWindowTicks + 20))
                .pos(cx + 60, y).size(40, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .pos(cx - 100, this.height - 27).size(200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xC0000000);
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        ctx.centeredText(this.getFont(), this.title, cx, 15, 0xFFFFFF);
        ctx.centeredText(this.getFont(),
                Component.literal("Attribution Window: " + LootJournalConfig.attributionWindowTicks + " ticks"),
                cx, windowLabelY, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        LootJournalConfig.save();
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void addToggle(int cx, int y, String label, boolean initial, Consumer<Boolean> setter) {
        boolean[] state = {initial};
        this.addRenderableWidget(Button.builder(
                toggleText(label, state[0]),
                btn -> {
                    state[0] = !state[0];
                    setter.accept(state[0]);
                    btn.setMessage(toggleText(label, state[0]));
                }
        ).pos(cx - 100, y).size(200, 20).build());
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "§aON" : "§cOFF"));
    }

    private static Component brModeText() {
        return Component.literal("Block Randomizer Mode: " +
                (LootJournalConfig.blockRandomizerMode ? "§aON" : "§cOFF"));
    }

    private static Component brFeedbackText() {
        return Component.literal("Drop Feedback in Chat: " +
                (LootJournalConfig.brFeedbackInChat ? "§aON" : "§cOFF"));
    }
}
