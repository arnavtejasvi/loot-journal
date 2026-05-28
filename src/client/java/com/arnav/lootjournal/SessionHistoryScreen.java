package com.arnav.lootjournal;

import com.arnav.lootjournal.output.JsonExporter;
import com.arnav.lootjournal.session.SessionReport;
import com.arnav.lootjournal.session.SessionTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class SessionHistoryScreen extends Screen {
    private static final int DETAIL_HEIGHT = 80;
    private static final int HEADER_HEIGHT = 28;
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Screen parent;
    private SessionList sessionList;
    private SessionReport selected;
    private int sessionCount = 0;

    public SessionHistoryScreen(Screen parent) {
        super(Text.literal("Session History"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<SessionReport> sessions = JsonExporter.loadRecentSessions(50);
        this.sessionCount = sessions.size();
        int listHeight = this.height - HEADER_HEIGHT - DETAIL_HEIGHT - 28;
        this.sessionList = new SessionList(this.client, this.width, listHeight, HEADER_HEIGHT, 22, sessions, this);
        this.addDrawableChild(sessionList);
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE,
                btn -> this.close())
                .position(this.width / 2 - 100, this.height - 24).size(200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);

        int detailTop = this.height - DETAIL_HEIGHT - 28;
        ctx.fill(0, detailTop, this.width, detailTop + DETAIL_HEIGHT, 0x88000000);
        ctx.drawHorizontalLine(0, this.width, detailTop, 0xFF555555);

        if (selected != null) {
            renderDetail(ctx, selected, detailTop + 4);
        } else {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(sessionCount == 0
                            ? "No sessions found. Enable writeSessionJson in settings."
                            : "Click a session to see details"),
                    this.width / 2, detailTop + DETAIL_HEIGHT / 2 - 4, 0x888888);
        }
    }

    private void renderDetail(DrawContext ctx, SessionReport s, int y) {
        int x = 8;
        int col2 = this.width / 2;
        int rowH = 12;

        String duration = formatDuration(s);
        String header = (s.worldName != null ? s.worldName : "unknown") + "  •  " + formatDate(s.startTime) + "  •  " + duration;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(header), x, y, 0xFFFFAA);
        y += rowH + 1;

        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§aGained: §f" + SessionTracker.formatItemMap(s.totalGained != null ? s.totalGained : Map.of())),
                x, y, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§eXP: §f+" + s.xpGained), col2, y, 0xFFFFFF);
        y += rowH;

        if (s.mobKills != null && !s.mobKills.isEmpty()) {
            int total = s.mobKills.values().stream().mapToInt(Integer::intValue).sum();
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("§bMobs: §f" + formatKillsSummary(s.mobKills, total)), x, y, 0xFFFFFF);
        } else {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7No mob kills recorded"), x, y, 0xFFFFFF);
        }
        int blocks = s.blockAggregates != null ? s.blockAggregates.size() : 0;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Blocks: §f" + blocks + " type(s)"), col2, y, 0xFFFFFF);
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
    public void close() {
        this.client.setScreen(parent);
    }

    // ─── Inner list widget ────────────────────────────────────────────────────

    static class SessionList extends EntryListWidget<SessionList.SessionEntry> {
        SessionList(MinecraftClient client, int width, int height, int top, int itemHeight,
                    List<SessionReport> sessions, SessionHistoryScreen screen) {
            super(client, width, height, top, itemHeight);
            for (SessionReport s : sessions) {
                this.addEntry(new SessionEntry(s, screen));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}

        static class SessionEntry extends EntryListWidget.Entry<SessionEntry> {
            private final SessionReport session;
            private final SessionHistoryScreen screen;

            SessionEntry(SessionReport session, SessionHistoryScreen screen) {
                this.session = session;
                this.screen = screen;
            }

            @Override
            public void render(DrawContext ctx, int index, int y, boolean hovered, float tickDelta) {
                MinecraftClient mc = MinecraftClient.getInstance();
                int x = getX();
                String date = session.startTime != null ? shortDate(session.startTime) : "?";
                String world = session.worldName != null ? session.worldName : "unknown";
                String dur = SessionHistoryScreen.formatDuration(session);
                int gained = session.totalGained != null
                        ? session.totalGained.values().stream().mapToInt(Integer::intValue).sum() : 0;
                int mobs = session.mobKills != null
                        ? session.mobKills.values().stream().mapToInt(Integer::intValue).sum() : 0;

                ctx.drawTextWithShadow(mc.textRenderer, Text.literal("§e" + date + "  §f" + world), x + 2, y + 2, 0xFFFFFF);
                ctx.drawTextWithShadow(mc.textRenderer,
                        Text.literal("§7" + dur + "  §a+" + gained + " items  §b" + mobs + " mobs"),
                        x + 2, y + 12, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(Click click, boolean bl) {
                screen.selectSession(session);
                screen.sessionList.setSelected(this);
                return true;
            }

            public Text getNarration() {
                return Text.literal(session.worldName != null ? session.worldName : "Session");
            }

            private static String shortDate(String iso) {
                try {
                    return DateTimeFormatter.ofPattern("MM-dd HH:mm").format(
                            LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e) {
                    return iso.length() > 10 ? iso.substring(0, 10) : iso;
                }
            }
        }
    }
}
