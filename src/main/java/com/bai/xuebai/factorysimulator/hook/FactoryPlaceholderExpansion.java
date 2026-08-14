package com.bai.xuebai.factorysimulator.hook;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class FactoryPlaceholderExpansion extends PlaceholderExpansion {
    private final FactorySimulator plugin;
    public FactoryPlaceholderExpansion(FactorySimulator plugin) { this.plugin = plugin; }
    public String getIdentifier() { return "factorysimulator"; }
    public String getAuthor() { return "XueBaiXD"; }
    public String getVersion() { return plugin.getDescription().getVersion(); }
    public boolean persist() { return true; }
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        FactoryProfile profile = plugin.getFactoryService().getOrCreate(player);
        String key = params == null ? "" : params.trim().toLowerCase(java.util.Locale.ROOT);
        if ("money".equals(key) || "balance".equals(key)) return String.format(java.util.Locale.ROOT, "%.2f", profile.getMoney());
        if ("level".equals(key) || "factory_level".equals(key)) return String.valueOf(profile.getLevel());
        if ("factory_name".equals(key) || "name".equals(key)) return profile.getFactoryName();
        if ("plot_size".equals(key) || "size".equals(key)) return String.valueOf(profile.getPlotSize());
        if ("workers".equals(key) || "worker_count".equals(key)) return String.valueOf(profile.getWorkers());
        if ("machines".equals(key) || "machine_count".equals(key)) return String.valueOf(profile.getMachines());
        if ("created".equals(key)) return String.valueOf(profile.isCreated());
        if ("rank_money".equals(key)) return String.valueOf(rank(profile, true));
        if ("rank_level".equals(key)) return String.valueOf(rank(profile, false));
        return null;
    }
    private int rank(FactoryProfile target, boolean money) {
        int rank = 1;
        for (FactoryProfile profile : plugin.getStorage().getAll()) {
            if (profile == target) continue;
            if ((money && profile.getMoney() > target.getMoney()) || (!money && profile.getLevel() > target.getLevel())) rank++;
        }
        return rank;
    }
}