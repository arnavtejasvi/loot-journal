package com.arnav.lootjournal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class LootJournalConfigScreen extends Screen {
    private final Screen parent;
    private int windowLabelY;

    public LootJournalConfigScreen(Screen parent) {
        super(Text.literal("Loot Journal Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx    = this.width / 2;
        int y     = 32;
        int gap   = 24;

        // Master toggle
        addToggle(cx, y, "Loot Journal Enabled", LootJournalConfig.enabled,
                v -> LootJournalConfig.enabled = v);
        y += gap;

        // ── Block Randomizer Mode ─────────────────────────────────────────────
        // Rebuilds the screen so the dependent BR feedback button reflects the new state.
        this.addDrawableChild(ButtonWidget.builder(brModeText(), btn -> {
            LootJournalConfig.blockRandomizerMode = !LootJournalConfig.blockRandomizerMode;
            if (LootJournalConfig.blockRandomizerMode
                    && LootJournalConfig.attributionWindowTicks < 100) {
                LootJournalConfig.attributionWindowTicks = 100;
            }
            this.clearAndInit();
        }).position(cx - 100, y).size(200, 20).build());
        y += gap;

        // BR feedback — greyed out when BR mode is off
        ButtonWidget brFeedbackBtn = ButtonWidget.builder(brFeedbackText(), btn -> {
            LootJournalConfig.brFeedbackInChat = !LootJournalConfig.brFeedbackInChat;
            btn.setMessage(brFeedbackText());
        }).position(cx - 100, y).size(200, 20).build();
        brFeedbackBtn.active = LootJournalConfig.blockRandomizerMode;
        this.addDrawableChild(brFeedbackBtn);
        y += gap;

        // ── General output options ────────────────────────────────────────────
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

        // ── Attribution window ±20 ticks ──────────────────────────────────────
        windowLabelY = y + 5;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("  -  "), btn ->
                LootJournalConfig.attributionWindowTicks =
                        Math.max(20, LootJournalConfig.attributionWindowTicks - 20))
                .position(cx - 100, y).size(40, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("  +  "), btn ->
                LootJournalConfig.attributionWindowTicks =
                        Math.min(200, LootJournalConfig.attributionWindowTicks + 20))
                .position(cx + 60, y).size(40, 20).build());

        // ── Done ─────────────────────────────────────────────────────────────
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, btn -> close())
                .position(cx - 100, this.height - 27).size(200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 15, 0xFFFFFF);
        // Attribution window label — redrawn every frame so it reflects live value
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Attribution Window: " + LootJournalConfig.attributionWindowTicks + " ticks"),
                this.width / 2, windowLabelY, 0xAAAAAA);
    }

    @Override
    public void close() {
        LootJournalConfig.save();
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addToggle(int cx, int y, String label, boolean initial, Consumer<Boolean> setter) {
        boolean[] state = {initial};
        this.addDrawableChild(ButtonWidget.builder(
                toggleText(label, state[0]),
                btn -> {
                    state[0] = !state[0];
                    setter.accept(state[0]);
                    btn.setMessage(toggleText(label, state[0]));
                }
        ).position(cx - 100, y).size(200, 20).build());
    }

    private static Text toggleText(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "§aON" : "§cOFF"));
    }

    private static Text brModeText() {
        return Text.literal("Block Randomizer Mode: " +
                (LootJournalConfig.blockRandomizerMode ? "§aON" : "§cOFF"));
    }

    private static Text brFeedbackText() {
        return Text.literal("Drop Feedback in Chat: " +
                (LootJournalConfig.brFeedbackInChat ? "§aON" : "§cOFF"));
    }
}
