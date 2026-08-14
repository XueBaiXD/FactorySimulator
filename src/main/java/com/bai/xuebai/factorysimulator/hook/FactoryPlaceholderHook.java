package com.bai.xuebai.factorysimulator.hook;

import com.bai.xuebai.factorysimulator.FactorySimulator;

public final class FactoryPlaceholderHook {
    private FactoryPlaceholderHook() {
    }

    public static boolean register(FactorySimulator plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return false;
        }
        return new FactoryPlaceholderExpansion(plugin).register();
    }
}
