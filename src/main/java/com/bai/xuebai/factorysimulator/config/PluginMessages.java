package com.bai.xuebai.factorysimulator.config;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginMessages {
    private final FactorySimulator plugin;
    private FileConfiguration selectorConfig;
    private FileConfiguration languageConfig;
    private String languageCode;

    public PluginMessages(FactorySimulator plugin) {
        this.plugin = plugin;
        reload();
    }

    public final void reload() {
        File selectorFile = new File(plugin.getDataFolder(), "messages.yml");
        this.selectorConfig = YamlConfiguration.loadConfiguration(selectorFile);
        this.languageCode = normalizeLanguage(selectorConfig.getString("language", "zh_cn"));

        File languageFile = new File(plugin.getDataFolder(), "lang/" + languageCode + ".yml");
        if (!languageFile.exists()) {
            String fallback = normalizeLanguage(selectorConfig.getString("fallback", "en"));
            languageFile = new File(plugin.getDataFolder(), "lang/" + fallback + ".yml");
            this.languageCode = fallback;
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String raw(String path) {
        String key = normalizePath(path);
        return languageConfig.getString(key, key);
    }

    public String prefix() {
        return colorWithoutPrefix(raw("prefix"));
    }

    public String get(String path) {
        return color(raw(path));
    }

    public String format(String path, Object... pairs) {
        String value = raw(path);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            value = value.replace("{" + String.valueOf(pairs[i]) + "}", String.valueOf(pairs[i + 1]));
        }
        return color(value);
    }

    public String color(String text) {
        String value = text == null ? "" : text;
        return ChatColor.translateAlternateColorCodes('&', value.replace("{prefix}", prefix()));
    }

    public List<String> help(String label) {
        List<String> lines = new ArrayList<String>();
        String header = languageConfig.getString("help.header");
        if (header != null && !header.trim().isEmpty()) {
            lines.add(formatValue(header, "label", label));
        }
        List<String> configured = languageConfig.getStringList("help.lines");
        for (String line : configured) {
            lines.add(formatValue(line, "label", label));
        }
        if (lines.isEmpty()) {
            String legacy = languageConfig.getString("help");
            if (legacy != null && !legacy.trim().isEmpty()) {
                lines.add(formatValue(legacy, "label", label));
            }
        }
        return Collections.unmodifiableList(lines);
    }

    private String formatValue(String value, Object... pairs) {
        String result = value == null ? "" : value;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result = result.replace("{" + String.valueOf(pairs[i]) + "}", String.valueOf(pairs[i + 1]));
        }
        return color(result);
    }

    private String colorWithoutPrefix(String text) {
        String value = text == null ? "" : text;
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String normalizeLanguage(String value) {
        return value == null ? "en" : value.trim().toLowerCase().replace('-', '_');
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        return path.startsWith("messages.") ? path.substring("messages.".length()) : path;
    }
}