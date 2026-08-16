package com.bai.xuebai.factorysimulator.service;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.config.PluginConfig;
import com.bai.xuebai.factorysimulator.config.PluginMessages;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.model.PlacedMachine;
import com.bai.xuebai.factorysimulator.storage.FactoryStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        applyOfflineIncome(profile);
        profile.setLastOnlineAt(System.currentTimeMillis());
        checkAchievements(profile);
        storage.save(profile);
        return profile;
    }

    private void applyOfflineIncome(FactoryProfile profile) {
        if (!profile.isCreated() || profile.getLastOnlineAt() <= 0) return;
        long elapsedMinutes = (System.currentTimeMillis() - profile.getLastOnlineAt()) / 60000L;
        int step = Math.max(1, config.getOfflineIncomeMinutes());
        long payableMinutes = (elapsedMinutes / step) * step;
        if (payableMinutes <= 0) return;
        int producers = 0;
        for (PlacedMachine machine : profile.getLayout()) {
            if (machine.getType().endsWith("_miner")) producers++;
        }
        if (producers <= 0) return;
        double cycles = payableMinutes * 60D / Math.max(1, config.getMachineProcessTicks() / 20D);
        double income = cycles * producers * config.getProductSellPrice("iron_ore") * config.getOfflineIncomeRate();
        profile.setOfflineStoredMoney(profile.getOfflineStoredMoney() + income);
        profile.setMoney(profile.getMoney() + profile.getOfflineStoredMoney());
        profile.setOfflineStoredMoney(0D);
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
        checkAchievements(profile);
        storage.save(profile);
    }

    public boolean hireWorker(Player player) {
        FactoryProfile profile = getOrCreate(player);
        if (!profile.isCreated() || profile.getWorkers() >= config.getMaxWorkers()
                || profile.getMoney() < config.getWorkerHireCost()) return false;
        profile.setMoney(profile.getMoney() - config.getWorkerHireCost());
        profile.setWorkers(profile.getWorkers() + 1);
        save(profile);
        return true;
    }

    public boolean fireWorker(Player player) {
        FactoryProfile profile = getOrCreate(player);
        if (!profile.isCreated() || profile.getWorkers() <= 0) return false;
        profile.setWorkers(profile.getWorkers() - 1);
        profile.setMoney(profile.getMoney() + config.getWorkerHireCost() * config.getWorkerFireRefundRate());
        save(profile);
        return true;
    }

    public double getMachineUpgradeCost(PlacedMachine machine) {
        return config.getMachineUpgradeCost(machine.getType(), machine.getLevel());
    }

    public boolean upgradeMachine(Player player, int x, int y, int z) {
        FactoryProfile profile = getOrCreate(player);
        if (!profile.isCreated()) return false;
        PlacedMachine machine = findMachine(profile, x, y, z);
        if (machine == null || machine.getLevel() >= config.getMaximumMachineLevel()) return false;
        double cost = getMachineUpgradeCost(machine);
        if (profile.getMoney() < cost) return false;
        profile.setMoney(profile.getMoney() - cost);
        machine.setLevel(machine.getLevel() + 1);
        save(profile);
        return true;
    }

    public List<FactoryProfile> leaderboard(boolean byLevel) {
        List<FactoryProfile> result = new ArrayList<>(storage.getAll());
        result.sort((left, right) -> {
            int comparison = byLevel ? Integer.compare(right.getLevel(), left.getLevel())
                    : Double.compare(right.getMoney(), left.getMoney());
            return comparison != 0 ? comparison : left.getPlayerName().compareToIgnoreCase(right.getPlayerName());
        });
        return result;
    }

    public int getRank(FactoryProfile target, boolean byLevel) {
        int rank = 1;
        for (FactoryProfile profile : storage.getAll()) {
            if (profile == target) continue;
            if ((byLevel && profile.getLevel() > target.getLevel())
                    || (!byLevel && profile.getMoney() > target.getMoney())) rank++;
        }
        return rank;
    }

    private void checkAchievements(FactoryProfile profile) {
        if (!config.isAchievementEnabled()) return;
        int machines = profile.getLayout().size();
        if (machines >= 1) profile.getAchievements().add("first_machine");
        if (machines >= 10) profile.getAchievements().add("industrial_park");
        if (profile.getMoney() >= 10000D) profile.getAchievements().add("first_ten_thousand");
        if (profile.getWorkers() >= 10) profile.getAchievements().add("team_builder");
        if (profile.getPlotSize() >= config.getMaxPlotSize()) profile.getAchievements().add("landlord");
        if (profile.getLevel() >= 10) profile.getAchievements().add("factory_tycoon");
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
            if (profile.isCreated()) profile.setLastOnlineAt(System.currentTimeMillis());
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