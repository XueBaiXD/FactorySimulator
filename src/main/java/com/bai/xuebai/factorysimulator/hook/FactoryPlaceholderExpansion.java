package com.bai.xuebai.factorysimulator.hook;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FactoryPlaceholderExpansion extends PlaceholderExpansion {
    private final FactorySimulator plugin;

    public FactoryPlaceholderExpansion(FactorySimulator plugin) {
        this.plugin = plugin;
    }

    public @NotNull String getIdentifier() {
        return "factorysimulator";
    }

    public @NotNull String getAuthor() {
        return "XueBaiXD";
    }

    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        FactoryProfile profile = plugin.getFactoryService().getOrCreate(player);
        String key = params.trim().toLowerCase(java.util.Locale.ROOT);
        switch (key) {
            case "money":
            case "balance":
                return String.format(java.util.Locale.ROOT, "%.2f", profile.getMoney());
            case "level":
            case "factory_level":
                return String.valueOf(profile.getLevel());
            case "factory_name":
            case "name":
                return profile.getFactoryName();
            case "plot_size":
            case "size":
                return String.valueOf(profile.getPlotSize());
            case "workers":
            case "worker_count":
                return String.valueOf(profile.getWorkers());
            case "machines":
            case "machine_count":
                return String.valueOf(profile.getMachines());
            case "created":
                return String.valueOf(profile.isCreated());
            case "rank_money":
            case "rank":
                return String.valueOf(plugin.getFactoryService().getRank(profile, false));
            case "rank_level":
                return String.valueOf(plugin.getFactoryService().getRank(profile, true));
            case "achievements":
            case "achievement_count":
                return String.valueOf(profile.getAchievements().size());
            case "offline_money":
                return String.format(java.util.Locale.ROOT, "%.2f", profile.getOfflineStoredMoney());
        }
        return null;
    }

}