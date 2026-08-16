package com.bai.xuebai.factorysimulator.service;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.config.PluginConfig;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.storage.FactoryStorage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WorldService {
    private final FactorySimulator plugin;
    private final PluginConfig config;
    private final FactoryStorage storage;

    public WorldService(FactorySimulator plugin, PluginConfig config, FactoryStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
    }

    public FactoryProfile ensureFactory(Player player) {
        FactoryProfile profile = storage.getOrCreate(player.getUniqueId(), player.getName());
        ensureWorld(profile);
        return profile;
    }

    public FactoryProfile ensureFactory(String playerName) {
        // 离线玩家只读信息时不强制创建新档案，交给存储层按需返回
        return storage.getByName(playerName);
    }

    public World ensureWorld(FactoryProfile profile) {
        if (profile == null) return null;
        String logicalName = logicalWorldName(profile);
        File folder = new File(plugin.getFactoryWorldDirectory(), logicalName);
        migrateLegacyWorld(logicalName, folder);
        World world = findLoadedWorld(profile, logicalName);
        if (world != null) {
            reinforceTemplate(world, profile);
            configureWorld(world);
            return world;
        }
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // WorldCreator(String) resolves relative names against the server root.
        // Passing the absolute plugin-owned folder prevents fs_* worlds from being
        // created beside the server's normal worlds.
        WorldCreator creator = new WorldCreator(folder.getAbsolutePath());
        creator.environment(worldEnvironment());
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new ChunkGenerator() {
            @Override
            public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                return createChunkData(world);
            }
        });
        world = Bukkit.createWorld(creator);
        if (world != null) {
            profile.setWorldName(world.getName());
            buildTemplate(world, profile.getPlotSize());
            restoreMachines(world, profile);
            storage.save(profile);
        }
        configureWorld(world);
        return world;
    }

    private void migrateLegacyWorld(String logicalName, File targetFolder) {
        if (targetFolder.exists()) return;
        File legacyFolder = new File(plugin.getServer().getWorldContainer(), logicalName);
        if (!legacyFolder.exists()) return;
        World loaded = Bukkit.getWorld(logicalName);
        if (loaded != null && !Bukkit.unloadWorld(loaded, true)) {
            plugin.getLogger().warning("无法迁移旧工厂世界，卸载失败: " + logicalName);
            return;
        }
        try {
            File parent = targetFolder.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.move(legacyFolder.toPath(), targetFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("已将旧工厂世界迁移至插件目录: " + logicalName);
        } catch (java.io.IOException exception) {
            plugin.getLogger().warning("迁移旧工厂世界失败: " + logicalName + " -> " + exception.getMessage());
        }
    }

    private String logicalWorldName(FactoryProfile profile) {
        String current = profile.getWorldName();
        if (current == null || current.trim().isEmpty()) {
            return "fs_" + profile.getPlayerId().replace("-", "");
        }
        File currentFolder = new File(current);
        if (currentFolder.isAbsolute()) return currentFolder.getName();
        return current;
    }

    private World findLoadedWorld(FactoryProfile profile, String logicalName) {
        World direct = Bukkit.getWorld(profile.getWorldName());
        if (direct != null) return direct;
        File expected;
        try {
            expected = new File(plugin.getFactoryWorldDirectory(), logicalName).getCanonicalFile();
            for (World loaded : Bukkit.getWorlds()) {
                if (loaded.getWorldFolder().getCanonicalFile().equals(expected)) return loaded;
            }
        } catch (java.io.IOException ignored) {
            return null;
        }
        return null;
    }

    public World getLoadedWorld(FactoryProfile profile) {
        if (profile == null) return null;
        return findLoadedWorld(profile, logicalWorldName(profile));
    }

    public Location getSpawnLocation(World world) {
        return new Location(world, 0.5D, config.getSpawnY(), 0.5D, 0F, 0F);
    }

    public void teleportToFactory(Player player, FactoryProfile profile) {
        World world = ensureWorld(profile);
        if (world != null) {
            player.teleport(getSpawnLocation(world));
        }
    }

    private void buildTemplate(World world, int plotSize) {
        clearToVoid(world, plotSize, java.util.Collections.<com.bai.xuebai.factorysimulator.model.PlacedMachine>emptyList());
        for (int x = -plotSize / 2; x <= plotSize / 2; x++) {
            for (int z = -plotSize / 2; z <= plotSize / 2; z++) {
                world.getBlockAt(x, 64, z).setType(config.getWorldPlatformMaterial());
            }
        }
        world.getBlockAt(0, 65, 0).setType(config.getWorldCenterMaterial());
        buildBoundary(world, plotSize);
    }

    private void configureWorld(World world) {
        if (world == null) return;
        world.setSpawnFlags(false, false);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("mobGriefing", "false");
    }

    private void reinforceTemplate(World world, FactoryProfile profile) {
        int plotSize = profile.getPlotSize();
        clearToVoid(world, plotSize, profile.getLayout());
        for (int x = -plotSize / 2; x <= plotSize / 2; x++) {
            for (int z = -plotSize / 2; z <= plotSize / 2; z++) {
                if (world.getBlockAt(x, 64, z).getType() == Material.AIR) {
                    world.getBlockAt(x, 64, z).setType(config.getWorldPlatformMaterial());
                }
            }
        }
        buildBoundary(world, plotSize);
    }

    private void buildBoundary(World world, int plotSize) {
        int half = plotSize / 2 + 1;
        int height = Math.min(world.getMaxHeight() - 1, config.getBorderHeight());
        for (int y = 64; y <= height; y++) {
            for (int coordinate = -half; coordinate <= half; coordinate++) {
                world.getBlockAt(-half, y, coordinate).setType(config.getWorldBorderMaterial());
                world.getBlockAt(half, y, coordinate).setType(config.getWorldBorderMaterial());
                world.getBlockAt(coordinate, y, -half).setType(config.getWorldBorderMaterial());
                world.getBlockAt(coordinate, y, half).setType(config.getWorldBorderMaterial());
            }
        }
    }

    private void clearToVoid(World world, int plotSize, Collection<com.bai.xuebai.factorysimulator.model.PlacedMachine> machines) {
        int radius = Math.max(config.getMaxPlotSize() / 2 + 8, plotSize / 2 + 8);
        int maxHeight = Math.min(world.getMaxHeight() - 1, config.getBorderHeight());
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                boolean platform = Math.abs(x) <= plotSize / 2 && Math.abs(z) <= plotSize / 2;
                for (int y = 0; y <= maxHeight; y++) {
                    if (platform && y == 64) continue;
                    if (containsMachine(machines, x, y, z)) continue;
                    if (world.getBlockAt(x, y, z).getType() != Material.AIR)
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
    }

    private boolean containsMachine(Collection<com.bai.xuebai.factorysimulator.model.PlacedMachine> machines, int x, int y, int z) {
        for (com.bai.xuebai.factorysimulator.model.PlacedMachine machine : machines) {
            if (machine.getX() == x && machine.getY() == y && machine.getZ() == z) return true;
        }
        return false;
    }

    private World.Environment worldEnvironment() {
        try {
            return World.Environment.valueOf(config.getWorldEnvironment().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return World.Environment.NORMAL;
        }
    }

    private void restoreMachines(World world, FactoryProfile profile) {
        Map<String, Material> materials = new HashMap<>();
        materials.put("basic_miner", Material.DISPENSER);
        materials.put("conveyor", Material.HOPPER);
        materials.put("coal_miner", Material.DROPPER);
        materials.put("iron_miner", Material.DISPENSER);
        materials.put("gold_miner", Material.DISPENSER);
        materials.put("redstone_miner", Material.DROPPER);
        materials.put("diamond_miner", Material.DISPENSER);
        materials.put("smelter", Material.FURNACE);
        materials.put("assembler", Material.DROPPER);
        materials.put("electric_furnace", Material.FURNACE);
        materials.put("fuel_generator", Material.REDSTONE_BLOCK);
        materials.put("storage", Material.CHEST);
        materials.put("seller", Material.CHEST);
        for (com.bai.xuebai.factorysimulator.model.PlacedMachine machine : profile.getLayout()) {
            Material material = materials.get(machine.getType());
            if (material != null) world.getBlockAt(machine.getX(), machine.getY(), machine.getZ()).setType(material);
        }
    }
}