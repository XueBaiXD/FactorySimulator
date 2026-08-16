package com.bai.xuebai.factorysimulator.storage.yaml;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.config.PluginConfig;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.model.PlacedMachine;
import com.bai.xuebai.factorysimulator.storage.FactoryStorage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlFactoryStorage implements FactoryStorage {
    private final FactorySimulator plugin;
    private final File folder;
    private final Map<String, FactoryProfile> cache = new HashMap<>();

    public YamlFactoryStorage(FactorySimulator plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "data");
    }

    @Override
    public void load() {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".yml")) {
                load(file);
            }
        }
    }

    @Override
    public void saveAll() {
        for (FactoryProfile profile : new ArrayList<>(cache.values())) {
            save(profile);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public FactoryProfile getOrCreate(UUID uuid, String playerName) {
        FactoryProfile profile = cache.get(uuid.toString());
        if (profile == null) {
            profile = load(uuid.toString());
        }
        if (profile == null) {
            profile = create(uuid.toString(), playerName);
        }
        profile.setPlayerName(playerName);
        cache.put(uuid.toString(), profile);
        return profile;
    }

    @Override
    public FactoryProfile getById(String id) {
        FactoryProfile profile = cache.get(id);
        return profile != null ? profile : load(id);
    }

    @Override
    public FactoryProfile getByName(String name) {
        for (FactoryProfile profile : cache.values()) {
            if (profile.getPlayerName() != null && profile.getPlayerName().equalsIgnoreCase(name)) {
                return profile;
            }
        }
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            FactoryProfile profile = load(file);
            if (profile != null && profile.getPlayerName() != null && profile.getPlayerName().equalsIgnoreCase(name)) {
                cache.put(profile.getPlayerId(), profile);
                return profile;
            }
        }
        return null;
    }

    @Override
    public Collection<FactoryProfile> getAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public void save(FactoryProfile profile) {
        if (profile == null || profile.getPlayerId() == null) {
            return;
        }
        File file = new File(folder, profile.getPlayerId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("playerId", profile.getPlayerId());
        yaml.set("playerName", profile.getPlayerName());
        yaml.set("worldName", profile.getWorldName());
        yaml.set("factoryName", profile.getFactoryName());
        yaml.set("created", profile.isCreated());
        yaml.set("createdAt", profile.getCreatedAt());
        yaml.set("lastOnlineAt", profile.getLastOnlineAt());
        yaml.set("lastClaimAt", profile.getLastClaimAt());
        yaml.set("level", profile.getLevel());
        yaml.set("plotSize", profile.getPlotSize());
        yaml.set("money", profile.getMoney());
        yaml.set("offlineStoredMoney", profile.getOfflineStoredMoney());
        yaml.set("workers", profile.getWorkers());
        yaml.set("machines", profile.getMachines());
        yaml.set("achievements", new ArrayList<>(profile.getAchievements()));
        yaml.set("unlockedMachines", profile.getUnlockedMachines());
        ArrayList<Map<String, Object>> layout = new ArrayList<>();
        for (PlacedMachine machine : profile.getLayout()) {
            Map<String, Object> value = new HashMap<>();
            value.put("type", machine.getType());
            value.put("x", machine.getX());
            value.put("y", machine.getY());
            value.put("z", machine.getZ());
            value.put("facing", machine.getFacing());
            value.put("level", machine.getLevel());
            value.put("progress", machine.getProgress());
            value.put("inventory", machine.getInventory());
            layout.add(value);
        }
        yaml.set("layout", layout);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存玩家工厂数据失败: " + profile.getPlayerId() + " -> " + e.getMessage());
        }
    }

    @Override
    public boolean exists(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml").exists();
    }

    private FactoryProfile create(String id, String playerName) {
        PluginConfig config = plugin.getPluginConfig();
        FactoryProfile profile = new FactoryProfile();
        profile.setPlayerId(id);
        profile.setPlayerName(playerName);
        profile.setWorldName("fs_" + id.replace("-", ""));
        profile.setFactoryName(playerName + "的工厂");
        profile.setCreated(false);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setLastOnlineAt(System.currentTimeMillis());
        profile.setLastClaimAt(System.currentTimeMillis());
        profile.setLevel(1);
        profile.setPlotSize(config.getInitialPlotSize());
        profile.setMoney(config.getStartingMoney());
        profile.setOfflineStoredMoney(0D);
        profile.setWorkers(config.getStartingWorkers());
        profile.setMachines(0);
        profile.getUnlockedMachines().add("basic_miner");
        save(profile);
        return profile;
    }

    private FactoryProfile load(String id) {
        return load(new File(folder, id + ".yml"));
    }

    private FactoryProfile load(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        FactoryProfile profile = new FactoryProfile();
        profile.setPlayerId(yaml.getString("playerId", file.getName().replace(".yml", "")));
        profile.setPlayerName(yaml.getString("playerName", ""));
        profile.setWorldName(yaml.getString("worldName", "fs_" + profile.getPlayerId().replace("-", "")));
        profile.setFactoryName(yaml.getString("factoryName", profile.getPlayerName() + "的工厂"));
        profile.setCreated(yaml.getBoolean("created", false));
        profile.setCreatedAt(yaml.getLong("createdAt", System.currentTimeMillis()));
        profile.setLastOnlineAt(yaml.getLong("lastOnlineAt", System.currentTimeMillis()));
        profile.setLastClaimAt(yaml.getLong("lastClaimAt", System.currentTimeMillis()));
        profile.setLevel(yaml.getInt("level", 1));
        profile.setPlotSize(yaml.getInt("plotSize", 32));
        profile.setMoney(yaml.getDouble("money", 1000D));
        profile.setOfflineStoredMoney(yaml.getDouble("offlineStoredMoney", 0D));
        profile.setWorkers(yaml.getInt("workers", 0));
        profile.setMachines(yaml.getInt("machines", 0));
        profile.getAchievements().addAll(yaml.getStringList("achievements"));
        profile.getUnlockedMachines().addAll(yaml.getStringList("unlockedMachines"));
        for (Map<?, ?> rawValue : yaml.getMapList("layout")) {
            Map<String, Object> value = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawValue.entrySet()) {
                value.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            PlacedMachine machine = new PlacedMachine();
            machine.setType(String.valueOf(value.get("type")));
            machine.setX(number(value.get("x")));
            machine.setY(number(value.get("y")));
            machine.setZ(number(value.get("z")));
            machine.setFacing(String.valueOf(value.get("facing")));
            machine.setLevel(number(value.get("level")));
            machine.setProgress(longNumber(value.get("progress")));
            Object inventory = value.get("inventory");
            if (inventory instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) inventory).entrySet()) {
                    machine.getInventory().put(String.valueOf(entry.getKey()), number(entry.getValue()));
                }
            }
            profile.getLayout().add(machine);
        }
        cache.put(profile.getPlayerId(), profile);
        return profile;
    }

    private int number(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private long longNumber(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}