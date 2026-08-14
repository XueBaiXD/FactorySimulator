package com.bai.xuebai.factorysimulator;

import com.bai.xuebai.factorysimulator.command.FactoryCommand;
import com.bai.xuebai.factorysimulator.config.PluginConfig;
import com.bai.xuebai.factorysimulator.config.PluginMessages;
import com.bai.xuebai.factorysimulator.listener.PlayerJoinListener;
import com.bai.xuebai.factorysimulator.listener.FactoryBlockListener;
import com.bai.xuebai.factorysimulator.listener.FactoryMenuListener;
import com.bai.xuebai.factorysimulator.listener.FactoryWorldListener;
import com.bai.xuebai.factorysimulator.service.FactoryService;
import com.bai.xuebai.factorysimulator.service.LibraryManager;
import com.bai.xuebai.factorysimulator.service.MachineRegistry;
import com.bai.xuebai.factorysimulator.service.FactoryTicker;
import com.bai.xuebai.factorysimulator.service.WorldService;
import com.bai.xuebai.factorysimulator.storage.FactoryStorage;
import com.bai.xuebai.factorysimulator.storage.StorageFactory;
import com.bai.xuebai.factorysimulator.platform.RuntimePlatform;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import com.bai.xuebai.factorysimulator.hook.FactoryPlaceholderExpansion;

public final class FactorySimulator extends JavaPlugin {

    private static FactorySimulator instance;

    private PluginConfig pluginConfig;
    private PluginMessages pluginMessages;
    private FactoryStorage storage;
    private FactoryService factoryService;
    private WorldService worldService;
    private LibraryManager libraryManager;
    private MachineRegistry machineRegistry;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("运行时兼容模式已启用: " + RuntimePlatform.serverVersion());
        getLogger().info("Bukkit API 包版本: " + RuntimePlatform.bukkitPackageVersion());

        saveBundledResources();

        this.pluginConfig = new PluginConfig(this);
        this.pluginMessages = new PluginMessages(this);

        this.libraryManager = new LibraryManager(this);
        this.libraryManager.bootstrap();

        this.storage = StorageFactory.create(this, pluginConfig);
        this.storage.load();

        this.worldService = new WorldService(this, pluginConfig, storage);
        this.factoryService = new FactoryService(this, pluginConfig, pluginMessages, storage, worldService);
        this.machineRegistry = new MachineRegistry(pluginConfig);
        if (pluginConfig.isPlaceholderApiEnabled()) {
            FactoryPlaceholderExpansion expansion = new FactoryPlaceholderExpansion(this);
            if (expansion.register()) getLogger().info("PlaceholderAPI 变量已注册：%factorysimulator_<变量名>%");
            else getLogger().warning("PlaceholderAPI 变量注册失败，请确认 PlaceholderAPI 已正确安装。");
        }

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(factoryService), this);
        Bukkit.getPluginManager().registerEvents(new FactoryBlockListener(factoryService, machineRegistry), this);
        Bukkit.getPluginManager().registerEvents(new FactoryMenuListener(this, factoryService, machineRegistry), this);
        Bukkit.getPluginManager().registerEvents(new FactoryWorldListener(factoryService), this);
        Bukkit.getScheduler().runTaskTimer(this, new FactoryTicker(this, pluginConfig, factoryService), pluginConfig.getMachineTickInterval(), pluginConfig.getMachineTickInterval());

        factoryService.bootstrapOnlinePlayers();
        getLogger().info("FactorySimulator 已启动，存储类型: " + pluginConfig.getStorageType().name());
    }

    @Override
    public void onDisable() {
        if (factoryService != null) {
            factoryService.shutdown();
        }
        if (storage != null) {
            storage.saveAll();
            storage.close();
        }
        instance = null;
    }

    private void registerCommands() {
        FactoryCommand command = new FactoryCommand(this, factoryService, pluginMessages, machineRegistry);
        PluginCommand fs = getCommand("fs");
        if (fs != null) {
            fs.setExecutor(command);
            fs.setTabCompleter(command);
        }
        PluginCommand factory = getCommand("factorysimulator");
        if (factory != null) {
            factory.setExecutor(command);
            factory.setTabCompleter(command);
        }
    }

    public void reloadConfigs() {
        pluginConfig.reload();
        pluginMessages.reload();
        machineRegistry.reload();
    }

    private void saveBundledResources() {
        saveDefaultConfig();
        saveResource("storage.yml", false);
        saveResource("factory.yml", false);
        saveResource("world.yml", false);
        saveResource("features.yml", false);
        saveResource("messages.yml", false);
        saveResource("lang/en.yml", false);
        saveResource("lang/zh_cn.yml", false);
        saveResource("lang/zh_tw.yml", false);
    }

    public static FactorySimulator getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public PluginMessages getPluginMessages() {
        return pluginMessages;
    }

    public FactoryStorage getStorage() {
        return storage;
    }

    public FactoryService getFactoryService() {
        return factoryService;
    }

    public WorldService getWorldService() {
        return worldService;
    }

    public File getFactoryWorldDirectory() {
        return new File(getDataFolder(), "world");
    }

    public File getFactoryDataDirectory() {
        return new File(getDataFolder(), "data");
    }

    public MachineRegistry getMachineRegistry() { return machineRegistry; }
}