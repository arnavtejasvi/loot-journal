package com.arnav.lootjournal;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

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
            if (LootJournalConfig.blockRandomizerMode && LootJournalConfig.attributionWindowTicks < 100) {
                LootJournalConfig.attributionWindowTicks = 100;
            }
            this.rebuildWidgets();
        }).bounds(cx - 100, y, 200, 20).build());
        y += gap;

        Button brFeedbackBtn = Button.builder(brFeedbackText(), btn -> {
            LootJournalConfig.brFeedbackInChat = !LootJournalConfig.brFeedbackInChat;
            btn.setMessage(brFeedbackText());
        }).bounds(cx - 100, y, 200, 20).build();
        brFeedbackBtn.active = LootJournalConfig.blockRandomizerMode;
        this.addRenderableWidget(brFeedbackBtn);
        y += gap;

        addToggle(cx, y, "Summary on Disconnect", LootJournalConfig.showSummaryOnDisconnect,
                v -> LootJournalConfig.showSummaryOnDisconnect = v);
        y += gap;

        addToggle(cx, y, "Write Session JSON", LootJournalConfig.writeSessionJson,
                v -> LootJournalConfig.writeSessionJson = v);
        y += gap;

        addToggle(cx, y, "Write Statistics JSON", LootJournalConfig.writeStatisticsJson,
                v -> LootJournalConfig.writeStatisticsJson = v);
        y += gap;

        addToggle(cx, y, "Write Per-Break Events", LootJournalConfig.writeBreakEvents,
                v -> LootJournalConfig.writeBreakEvents = v);
        y += gap;

        windowLabelY = y + 5;
        this.addRenderableWidget(Button.builder(Component.literal("  -  "), btn ->
                LootJournalConfig.attributionWindowTicks =
                        Math.max(20, LootJournalConfig.attributionWindowTicks - 20))
                .bounds(cx - 100, y, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("  +  "), btn ->
                LootJournalConfig.attributionWindowTicks =
                        Math.min(200, LootJournalConfig.attributionWindowTicks + 20))
                .bounds(cx + 60, y, 40, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                .bounds(cx - 100, this.height - 27, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        int titleW = this.font.width(this.title);
        guiGraphics.text(this.font, this.title, this.width / 2 - titleW / 2, 15, 0xFFFFFF);
        Component attrLabel = Component.literal("Attribution Window: " + LootJournalConfig.attributionWindowTicks + " ticks");
        int attrW = this.font.width(attrLabel);
        guiGraphics.text(this.font, attrLabel, this.width / 2 - attrW / 2, windowLabelY, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        LootJournalConfig.save();
        this.minecraft.setScreen(this.parent);
    }

    private void addToggle(int cx, int y, String label, boolean initial, Consumer<Boolean> setter) {
        boolean[] state = {initial};
        this.addRenderableWidget(Button.builder(toggleText(label, state[0]), btn -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            btn.setMessage(toggleText(label, state[0]));
        }).bounds(cx - 100, y, 200, 20).build());
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
