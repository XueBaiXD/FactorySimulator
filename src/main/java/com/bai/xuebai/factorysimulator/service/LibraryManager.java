package com.bai.xuebai.factorysimulator.service;

import com.bai.xuebai.factorysimulator.FactorySimulator;

import java.io.File;

public class LibraryManager {
    private final FactorySimulator plugin;
    private final File libsFolder;

    public LibraryManager(FactorySimulator plugin) {
        this.plugin = plugin;
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File serverRoot = pluginsFolder != null ? pluginsFolder.getParentFile() : plugin.getDataFolder().getParentFile();
        this.libsFolder = new File(serverRoot != null ? serverRoot : plugin.getDataFolder(), "libraries");
    }

    public void bootstrap() {
        if (!libsFolder.exists()) {
            libsFolder.mkdirs();
        }
        if (isAvailable("org.sqlite.JDBC")
                && (isAvailable("com.mysql.cj.jdbc.Driver") || isAvailable("com.mysql.jdbc.Driver"))) {
            plugin.getLogger().info("已使用插件内置 JDBC 驱动。");
        } else {
            plugin.getLogger().warning("未检测到完整的内置 JDBC 驱动，请检查最终 JAR 是否经过 shade/package 构建。");
        }
    }

    private boolean isAvailable(String className) {
        try {
            Class.forName(className, false, plugin.getClass().getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}