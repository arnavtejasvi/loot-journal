package com.arnav.lootjournal;

import com.arnav.lootjournal.output.JsonExporter;
import com.arnav.lootjournal.session.SessionReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class SessionHistoryScreen extends Screen {
    private static final int DETAIL_HEIGHT = 80;
    private static final int HEADER_HEIGHT = 28;
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Screen parent;
    private SessionList sessionList;
    private SessionReport selected;

    public SessionHistoryScreen(Screen parent) {
        super(Component.literal("Session History"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<SessionReport> sessions = JsonExporter.loadRecentSessions(50);
        int listHeight = this.height - HEADER_HEIGHT - DETAIL_HEIGHT - 28;
        this.sessionList = new SessionList(this.minecraft, this.width, listHeight, HEADER_HEIGHT, 22, sessions, this);
        this.addRenderableWidget(sessionList);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                btn -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 24, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        int detailTop = this.height - DETAIL_HEIGHT - 28;
        g.fill(0, detailTop, this.width, detailTop + DETAIL_HEIGHT, 0x88000000);
        g.hLine(0, this.width, detailTop, 0xFF555555);

        if (selected != null) {
            renderDetail(g, selected, detailTop + 4);
        } else if (sessionList.getEntryCount() == 0) {
            g.drawCenteredString(this.font,
                    Component.literal("No session history found. Sessions are saved when writeSessionJson is enabled."),
                    this.width / 2, HEADER_HEIGHT + 20, 0xAAAAAA);
            g.drawCenteredString(this.font,
                    Component.literal("Click a session to see details"),
                    this.width / 2, detailTop + DETAIL_HEIGHT / 2 - 4, 0x888888);
        } else {
            g.drawCenteredString(this.font,
                    Component.literal("Click a session to see details"),
                    this.width / 2, detailTop + DETAIL_HEIGHT / 2 - 4, 0x888888);
        }
    }

    private void renderDetail(GuiGraphics g, SessionReport s, int y) {
        int x = 8;
        int col2 = this.width / 2;
        int rowH = 12;

        String duration = formatDuration(s);
        String header = (s.worldName != null ? s.worldName : "unknown") + "  •  " + formatDate(s.startTime) + "  •  " + duration;
        g.drawString(this.font, header, x, y, 0xFFFFAA);
        y += rowH + 1;

        g.drawString(this.font, "§aGained: §f" + LootJournalSession.formatItemMap(s.totalGained != null ? s.totalGained : Map.of()), x, y, 0xFFFFFF);
        g.drawString(this.font, "§eXP: §f+" + s.xpGained, col2, y, 0xFFFFFF);
        y += rowH;

        if (s.mobKills != null && !s.mobKills.isEmpty()) {
            int total = s.mobKills.values().stream().mapToInt(Integer::intValue).sum();
            g.drawString(this.font, "§bMobs: §f" + formatKillsSummary(s.mobKills, total), x, y, 0xFFFFFF);
        } else {
            g.drawString(this.font, "§7No mob kills recorded", x, y, 0xFFFFFF);
        }
        int blocks = s.blockAggregates != null ? s.blockAggregates.size() : 0;
        g.drawString(this.font, "§7Blocks: §f" + blocks + " type(s)", col2, y, 0xFFFFFF);
    }

    static String formatDuration(SessionReport s) {
        long secs = s.durationSeconds > 0 ? s.durationSeconds : s.durationMinutes * 60;
        if (secs <= 0) return "?";
        long h = secs / 3600, m = (secs % 3600) / 60, sec = secs % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + sec + "s";
        return sec + "s";
    }

    static String formatDate(String iso) {
        if (iso == null) return "?";
        try {
            return DISPLAY_FMT.format(LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (DateTimeParseException e) {
            return iso.length() > 16 ? iso.substring(0, 16) : iso;
        }
    }

    private static String formatKillsSummary(Map<String, Integer> kills, int total) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> e : kills.entrySet()) {
            if (i > 0) sb.append(", ");
            String name = e.getKey().contains(":") ? e.getKey().substring(e.getKey().lastIndexOf(':') + 1) : e.getKey();
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1).replace('_', ' ');
            sb.append(name).append(" x").append(e.getValue());
            if (++i >= 3 && kills.size() > 3) { sb.append(" +more"); break; }
        }
        sb.append(" [").append(total).append("]");
        return sb.toString();
    }

    void selectSession(SessionReport session) {
        this.selected = session;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    // ─── Inner list widget ────────────────────────────────────────────────────

    static class SessionList extends ObjectSelectionList<SessionList.SessionEntry> {
        SessionList(Minecraft mc, int width, int height, int y, int itemHeight,
                    List<SessionReport> sessions, SessionHistoryScreen screen) {
            super(mc, width, height, y, itemHeight);
            for (SessionReport s : sessions) {
                this.addEntry(new SessionEntry(s, screen));
            }
        }

        public int getEntryCount() {
            return this.getItemCount();
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        static class SessionEntry extends ObjectSelectionList.Entry<SessionEntry> {
            private final SessionReport session;
            private final SessionHistoryScreen screen;

            SessionEntry(SessionReport session, SessionHistoryScreen screen) {
                this.session = session;
                this.screen = screen;
            }

            @Override
            public void renderContent(GuiGraphics g, int x, int y, boolean hovered, float delta) {
                Minecraft mc = Minecraft.getInstance();
                String date = session.startTime != null
                        ? DateTimeFormatter.ofPattern("MM-dd HH:mm").format(
                                LocalDateTime.parse(session.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        : "?";
                String world = session.worldName != null ? session.worldName : "unknown";
                String dur = SessionHistoryScreen.formatDuration(session);
                int gained = session.totalGained != null
                        ? session.totalGained.values().stream().mapToInt(Integer::intValue).sum() : 0;
                int mobs = session.mobKills != null
                        ? session.mobKills.values().stream().mapToInt(Integer::intValue).sum() : 0;

                g.drawString(mc.font, "§e" + date + "  §f" + world, x + 2, y + 2, 0xFFFFFF);
                g.drawString(mc.font, "§7" + dur + "  §a+" + gained + " items  §b" + mobs + " mobs",
                        x + 2, y + 12, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean p_432750_) {
                screen.selectSession(session);
                screen.sessionList.setSelected(this);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(session.worldName != null ? session.worldName : "Session");
            }
        }
    }
}
