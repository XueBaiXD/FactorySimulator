package com.bai.xuebai.factorysimulator.service;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.config.PluginConfig;
import com.bai.xuebai.factorysimulator.config.PluginMessages;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.model.PlacedMachine;
import com.bai.xuebai.factorysimulator.storage.FactoryStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

public class FactoryService {
    private final FactorySimulator plugin;
    private final PluginConfig config;
    private final PluginMessages messages;
    private final FactoryStorage storage;
    private final WorldService worldService;

    public FactoryService(FactorySimulator plugin, PluginConfig config, PluginMessages messages, FactoryStorage storage, WorldService worldService) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.storage = storage;
        this.worldService = worldService;
    }

    public FactoryProfile join(Player player) {
        FactoryProfile profile = storage.getOrCreate(player.getUniqueId(), player.getName());
        profile.setLastOnlineAt(System.currentTimeMillis());
        storage.save(profile);
        return profile;
    }

    public boolean createFactory(Player player) {
        FactoryProfile profile = storage.getOrCreate(player.getUniqueId(), player.getName());
        if (profile.isCreated()) return false;
        profile.setCreated(true);
        profile.setFactoryName(player.getName() + "的工厂");
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setLevel(1);
        profile.setPlotSize(config.getInitialPlotSize());
        profile.setMoney(config.getStartingMoney());
        profile.getUnlockedMachines().clear();
        profile.getUnlockedMachines().add("basic_miner");
        storage.save(profile);
        worldService.ensureWorld(profile);
        player.getInventory().addItem(plugin.getMachineRegistry().createItem("basic_miner", config.getInitialMachineAmount()));
        player.getInventory().addItem(plugin.getMachineRegistry().createItem("conveyor", config.getInitialConveyorAmount()));
        player.getInventory().addItem(plugin.getMachineRegistry().createItem("seller", config.getInitialPackageAmount()));
        player.getInventory().addItem(plugin.getMachineRegistry().createProtectedPickaxe());
        player.getInventory().addItem(plugin.getMachineRegistry().createTutorialBook());
        return true;
    }

    public void enterFactory(Player player) {
        FactoryProfile profile = storage.getOrCreate(player.getUniqueId(), player.getName());
        if (profile.isCreated()) worldService.teleportToFactory(player, profile);
    }

    public boolean renameFactory(Player player, String name) {
        FactoryProfile profile = storage.getOrCreate(player.getUniqueId(), player.getName());
        if (!profile.isCreated() || name == null || name.trim().isEmpty() || name.length() > 32) return false;
        profile.setFactoryName(name.trim());
        save(profile);
        return true;
    }

    public double getPlotUpgradeCost(FactoryProfile profile) {
        return config.getPlotUpgradeBaseCost() * Math.max(1D, profile.getPlotSize() / (double) config.getInitialPlotSize());
    }

    public boolean upgradePlot(Player player) {
        FactoryProfile profile = storage.getOrCreate(player.getUniqueId(), player.getName());
        if (!profile.isCreated()) return false;
        if (profile.getPlotSize() >= config.getMaxPlotSize()) return false;
        double cost = getPlotUpgradeCost(profile);
        if (profile.getMoney() < cost) return false;
        int nextSize = Math.min(config.getMaxPlotSize(), profile.getPlotSize() + config.getPlotGrowthPerLevel());
        profile.setMoney(profile.getMoney() - cost);
        profile.setPlotSize(nextSize);
        profile.setLevel(profile.getLevel() + 1);
        save(profile);
        worldService.ensureWorld(profile);
        return true;
    }

    public FactoryProfile getOrCreate(Player player) {
        return storage.getOrCreate(player.getUniqueId(), player.getName());
    }

    public FactoryProfile getByPlayerName(String name) {
        return storage.getByName(name);
    }

    public FactoryProfile getById(String id) {
        return storage.getById(id);
    }

    public void save(FactoryProfile profile) {
        profile.setMachines(profile.getLayout().size());
        storage.save(profile);
    }

    public FactoryProfile getByWorld(String worldName) {
        for (FactoryProfile profile : storage.getAll()) {
            if (worldName.equals(profile.getWorldName())) return profile;
        }
        return null;
    }

    public boolean isInsidePlot(FactoryProfile profile, int x, int z) {
        int half = profile.getPlotSize() / 2;
        return Math.abs(x) <= half && Math.abs(z) <= half;
    }

    public PlacedMachine findMachine(FactoryProfile profile, int x, int y, int z) {
        for (PlacedMachine machine : profile.getLayout()) {
            if (machine.getX() == x && machine.getY() == y && machine.getZ() == z) return machine;
        }
        return null;
    }

    public PlacedMachine findAdjacent(FactoryProfile profile, PlacedMachine source) {
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            PlacedMachine target = findMachine(profile, source.getX() + offset[0], source.getY(), source.getZ() + offset[1]);
            if (target != null) return target;
        }
        return null;
    }

    public void bootstrapOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            join(player);
        }
    }

    public void shutdown() {
        Collection<FactoryProfile> all = storage.getAll();
        for (FactoryProfile profile : all) {
            storage.save(profile);
        }
    }

    public String formatOverview(FactoryProfile profile) {
        long since = Math.max(0L, System.currentTimeMillis() - profile.getLastOnlineAt());
        long offlineHours = since / 3600000L;
        return messages.format("info-player",
                "name", profile.getPlayerName(),
                "id", profile.getPlayerId(),
                "level", profile.getLevel(),
                "plotSize", profile.getPlotSize(),
                "money", String.format("%.2f", profile.getMoney()),
                "workers", profile.getWorkers(),
                "machines", profile.getMachines(),
                "offlineHours", offlineHours,
                "achievements", profile.getAchievements().size());
    }

    public String formatServerStatus() {
        int total = storage.getAll().size();
        int online = Bukkit.getOnlinePlayers().size();
        return messages.format("info-server",
                "players", total,
                "online", online,
                "storage", config.getStorageType().name(),
                "placeholderapi", config.isPlaceholderApiEnabled() ? "ON" : "OFF",
                "vault", config.isVaultEnabled() ? "ON" : "OFF",
                "hologram", config.isHologramsEnabled() ? "ON" : "OFF");
    }
}