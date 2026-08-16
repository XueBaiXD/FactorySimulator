package com.bai.xuebai.factorysimulator.config;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PluginConfig {
    private final FactorySimulator plugin;
    private FileConfiguration generalConfig;
    private FileConfiguration storageConfig;
    private FileConfiguration factoryConfig;
    private FileConfiguration worldConfig;
    private FileConfiguration featureConfig;

    public PluginConfig(FactorySimulator plugin) {
        this.plugin = plugin;
        this.generalConfig = load("config.yml");
        this.storageConfig = load("storage.yml");
        this.factoryConfig = load("factory.yml");
        this.worldConfig = load("world.yml");
        this.featureConfig = load("features.yml");
    }

    public void reload() {
        plugin.reloadConfig();
        this.generalConfig = load("config.yml");
        this.storageConfig = load("storage.yml");
        this.factoryConfig = load("factory.yml");
        this.worldConfig = load("world.yml");
        this.featureConfig = load("features.yml");
    }

    public StorageType getStorageType() {
        return StorageType.fromString(storageConfig.getString("storage.type", "YAML"));
    }

    public String getMysqlHost() {
        return storageConfig.getString("storage.mysql.host", "127.0.0.1");
    }

    public int getMysqlPort() {
        return storageConfig.getInt("storage.mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return storageConfig.getString("storage.mysql.database", "factorysimulator");
    }

    public String getMysqlUser() {
        return storageConfig.getString("storage.mysql.user", "root");
    }

    public String getMysqlPassword() {
        return storageConfig.getString("storage.mysql.password", "");
    }

    public String getMysqlParams() {
        return storageConfig.getString("storage.mysql.params", "useSSL=false&characterEncoding=utf8&autoReconnect=true");
    }

    public String getSqliteFileName() {
        return storageConfig.getString("storage.sqlite.file", "factorysimulator.db");
    }

    public int getStoragePoolSize() {
        return storageConfig.getInt("storage.pool-size", 8);
    }

    public int getInitialPlotSize() {
        return factoryConfig.getInt("plot.initial-size", 32);
    }

    public int getPlotGrowthPerLevel() {
        return factoryConfig.getInt("plot.growth-per-level", 8);
    }

    public int getMaxPlotSize() {
        return factoryConfig.getInt("plot.max-size", 256);
    }

    public double getPlotUpgradeBaseCost() {
        return factoryConfig.getDouble("plot.upgrade-base-cost", 2500D);
    }

    public double getProductSellPrice(String id) {
        return factoryConfig.getDouble("production.sell-prices." + id, getMachineSellPrice());
    }

    public int getStartingMoney() {
        return factoryConfig.getInt("economy.starting-money", 1000);
    }

    public int getInitialMachineAmount() {
        return factoryConfig.getInt("starter.initial-machine-amount", 1);
    }

    public int getInitialConveyorAmount() {
        return factoryConfig.getInt("starter.initial-conveyor-amount", 4);
    }

    public int getInitialPackageAmount() {
        return factoryConfig.getInt("starter.initial-package-amount", 1);
    }

    public int getPickaxeCooldown() {
        return factoryConfig.getInt("starter.pickaxe-cooldown", 1);
    }

    public double getMachinePrice(String id) {
        return factoryConfig.getDouble("shop.items." + id + ".price", 100D);
    }

    public int getMachineUnlockLevel(String id) {
        return factoryConfig.getInt("shop.items." + id + ".unlock-level", 1);
    }

    public int getLevelRequiredMoney() {
        return factoryConfig.getInt("leveling.money-per-level", 5000);
    }

    public int getMachineTickInterval() {
        return Math.max(1, factoryConfig.getInt("production.tick-interval", 20));
    }

    public int getMachineProcessTicks() {
        return Math.max(1, factoryConfig.getInt("production.basic-process-ticks", 100));
    }

    public double getMachineSellPrice() {
        return factoryConfig.getDouble("production.basic-sell-price", 8.0D);
    }

    public boolean isMachineDropsEnabled() {
        return factoryConfig.getBoolean("machines.drop-on-break", true);
    }

    public String getMachineMarker() {
        return factoryConfig.getString("machines.item-marker", "§0FS_MACHINE:");
    }

    public int getOfflineIncomeMinutes() {
        return factoryConfig.getInt("economy.offline-income-minute-step", 5);
    }

    public double getOfflineIncomeRate() {
        return factoryConfig.getDouble("economy.offline-income-rate", 0.25D);
    }

    public int getStartingWorkers() {
        return factoryConfig.getInt("workers.starting-count", 0);
    }

    public int getMaxWorkers() {
        return factoryConfig.getInt("workers.max-count", 32);
    }

    public double getWorkerBaseEfficiency() {
        return factoryConfig.getDouble("workers.base-efficiency", 1.0D);
    }

    public boolean isAchievementEnabled() {
        return factoryConfig.getBoolean("achievements.enabled", true);
    }

    public int getAchievementBroadcastThreshold() {
        return factoryConfig.getInt("achievements.broadcast-threshold", 10);
    }

    public int getBorderHeight() {
        return worldConfig.getInt("generation.border-height", 80);
    }

    public int getSpawnY() {
        return worldConfig.getInt("generation.spawn-y", 65);
    }

    public String getWorldEnvironment() {
        return worldConfig.getString("generation.environment", "NORMAL");
    }

    public boolean isAutoCreateWorldOnJoin() {
        return worldConfig.getBoolean("world.auto-create-on-join", true);
    }

    public boolean isUseVoidFloor() {
        return worldConfig.getBoolean("generation.use-void-floor", false);
    }

    public Material getWorldBorderMaterial() {
        return material(worldConfig, "generation.border-material", Material.STONE);
    }

    public Material getWorldFloorMaterial() {
        return material(worldConfig, "generation.floor-material", Material.BEDROCK);
    }

    public Material getWorldPlatformMaterial() {
        return material(worldConfig, "generation.platform-material", Material.GRASS);
    }

    public Material getWorldCenterMaterial() {
        return material(worldConfig, "generation.center-marker-material", Material.EMERALD_BLOCK);
    }

    public boolean isLeaderboardEnabled() {
        return featureConfig.getBoolean("leaderboard.enabled", true);
    }

    public int getLeaderboardSize() {
        return featureConfig.getInt("leaderboard.size", 10);
    }

    public boolean isHologramsEnabled() {
        return featureConfig.getBoolean("hooks.holographic-displays", true);
    }

    public boolean isPlaceholderApiEnabled() {
        return featureConfig.getBoolean("hooks.placeholderapi", true) && plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public boolean isVaultEnabled() {
        return featureConfig.getBoolean("hooks.vault", true) && plugin.getServer().getPluginManager().isPluginEnabled("Vault");
    }

    public boolean isMultiverseEnabled() {
        return featureConfig.getBoolean("hooks.multiverse-core", true) && plugin.getServer().getPluginManager().isPluginEnabled("Multiverse-Core");
    }

    public boolean isDebugEnabled() {
        return generalConfig.getBoolean("general.debug", false);
    }

    private FileConfiguration load(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }

    private Material material(FileConfiguration config, String path, Material fallback) {
        String raw = config.getString(path, fallback.name());
        try {
            return Material.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}