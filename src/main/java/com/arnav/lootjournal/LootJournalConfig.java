package com.arnav.lootjournal;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class LootJournalConfig {
    public static boolean enabled = true;
    public static int attributionWindowTicks = 60;
    public static boolean writeSessionJson = true;
    public static boolean writeStatisticsJson = true;
    public static boolean writeBreakEvents = true;
    public static boolean showSummaryOnDisconnect = true;
    public static boolean blockRandomizerMode = false;
    public static boolean brFeedbackInChat = true;

    private LootJournalConfig() {}

    private static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve("lootjournal.properties");
    }

    public static void load() {
        Path path = configFile();
        if (!Files.exists(path)) return;
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            System.err.println("[LootJournal] Failed to load config: " + e.getMessage());
            return;
        }
        enabled                = bool(props, "enabled", enabled);
        attributionWindowTicks = integer(props, "attributionWindowTicks", attributionWindowTicks);
        writeSessionJson       = bool(props, "writeSessionJson", writeSessionJson);
        writeStatisticsJson    = bool(props, "writeStatisticsJson", writeStatisticsJson);
        writeBreakEvents       = bool(props, "writeBreakEvents", writeBreakEvents);
        showSummaryOnDisconnect = bool(props, "showSummaryOnDisconnect", showSummaryOnDisconnect);
        blockRandomizerMode    = bool(props, "blockRandomizerMode", blockRandomizerMode);
        brFeedbackInChat       = bool(props, "brFeedbackInChat", brFeedbackInChat);
        if (blockRandomizerMode && attributionWindowTicks < 100) attributionWindowTicks = 100;
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("enabled", String.valueOf(enabled));
        props.setProperty("attributionWindowTicks", String.valueOf(attributionWindowTicks));
        props.setProperty("writeSessionJson", String.valueOf(writeSessionJson));
        props.setProperty("writeStatisticsJson", String.valueOf(writeStatisticsJson));
        props.setProperty("writeBreakEvents", String.valueOf(writeBreakEvents));
        props.setProperty("showSummaryOnDisconnect", String.valueOf(showSummaryOnDisconnect));
        props.setProperty("blockRandomizerMode", String.valueOf(blockRandomizerMode));
        props.setProperty("brFeedbackInChat", String.valueOf(brFeedbackInChat));
        try {
            Files.createDirectories(configFile().getParent());
            try (Writer w = Files.newBufferedWriter(configFile(), StandardCharsets.UTF_8)) {
                props.store(w, "Loot Journal Configuration");
            }
        } catch (IOException e) {
            System.err.println("[LootJournal] Failed to save config: " + e.getMessage());
        }
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v != null ? Boolean.parseBoolean(v) : def;
    }

    private static int integer(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }
}
