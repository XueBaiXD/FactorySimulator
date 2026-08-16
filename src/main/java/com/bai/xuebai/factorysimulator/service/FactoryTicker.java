package com.bai.xuebai.factorysimulator.service;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.config.PluginConfig;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.model.PlacedMachine;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class FactoryTicker implements Runnable {
    private final FactorySimulator plugin;
    private final PluginConfig config;
    private final FactoryService service;

    public FactoryTicker(FactorySimulator plugin, PluginConfig config, FactoryService service) {
        this.plugin = plugin;
        this.config = config;
        this.service = service;
    }

    @Override
    public void run() {
        for (FactoryProfile profile : plugin.getStorage().getAll()) {
            boolean changed = false;
            World world = plugin.getWorldService().getLoadedWorld(profile);
            if (world == null) continue;
            syncStorageFromWorld(profile, world);
            syncMachineContainersFromWorld(profile, world);
            syncConveyorsFromWorld(profile, world);
            for (PlacedMachine machine : profile.getLayout()) {
                if ("basic_miner".equals(machine.getType()) || "coal_miner".equals(machine.getType()) || "iron_miner".equals(machine.getType())) {
                    machine.setProgress(machine.getProgress() + config.getMachineTickInterval());
                    if (machine.getProgress() >= config.getMachineProcessTicks()) {
                        String product = "basic_miner".equals(machine.getType()) ? "iron_ore" : ("coal_miner".equals(machine.getType()) ? "coal" : "iron_ore");
                        if (produceToContainer(world, machine, product)) {
                            machine.setProgress(0);
                            changed = true;
                        }
                    }
                } else if ("gold_miner".equals(machine.getType()) || "redstone_miner".equals(machine.getType()) || "diamond_miner".equals(machine.getType())) {
                    machine.setProgress(machine.getProgress() + config.getMachineTickInterval());
                    if (machine.getProgress() >= config.getMachineProcessTicks()) {
                        String product = "gold_miner".equals(machine.getType()) ? "gold_ore" : ("redstone_miner".equals(machine.getType()) ? "redstone" : "diamond");
                        if (produceToContainer(world, machine, product)) {
                            machine.setProgress(0);
                            changed = true;
                        }
                    }
                } else if ("smelter".equals(machine.getType())) {
                    changed |= convert(machine, "iron_ore", "iron_ingot");
                    changed |= convert(machine, "ore", "iron_ingot");
                } else if ("assembler".equals(machine.getType())) {
                    changed |= convert(machine, "iron_ingot", "factory_product");
                } else if ("electric_furnace".equals(machine.getType())) {
                    changed |= convert(machine, "gold_ore", "gold_ingot");
                    changed |= convert(machine, "iron_ore", "iron_ingot");
                } else if ("seller".equals(machine.getType())) {
                    for (Map.Entry<String, Integer> entry : new java.util.HashMap<>(machine.getInventory()).entrySet()) {
                        Integer amount = entry.getValue();
                        if (amount != null && amount > 0) {
                            profile.setMoney(profile.getMoney() + amount * getSellPrice(entry.getKey()));
                            machine.getInventory().remove(entry.getKey());
                            changed = true;
                        }
                    }
                }
            }
            for (PlacedMachine machine : profile.getLayout()) {
                if ("storage".equals(machine.getType())) continue;
                for (String product : new java.util.ArrayList<>(machine.getInventory().keySet())) {
                    changed |= moveOne(profile, world, machine, product);
                }
            }
            syncMachineContainers(profile, world);
            if (changed) service.save(profile);
        }
    }

    private void spawnProduct(World world, PlacedMachine machine, String product) {
        ItemStack item = ProductItemService.create(product, 1, config.getProductSellPrice(product));
        if (item == null) return;
        Item entity = world.dropItem(new org.bukkit.Location(world, machine.getX() + 0.5D, machine.getY() + 1.15D, machine.getZ() + 0.5D), item);
        entity.setPickupDelay(0);
        entity.setVelocity(new org.bukkit.util.Vector(0D, 0.08D, 0D));
    }

    private boolean produceToContainer(World world, PlacedMachine machine, String product) {
        Block block = world.getBlockAt(machine.getX(), machine.getY(), machine.getZ());
        if (!(block.getState() instanceof org.bukkit.block.Container)) return false;
        ItemStack item = ProductItemService.create(product, 1, config.getProductSellPrice(product));
        if (item == null) return false;
        Inventory inventory = ((org.bukkit.block.Container) block.getState()).getInventory();
        java.util.Map<Integer, ItemStack> leftovers = inventory.addItem(item);
        int accepted = item.getAmount();
        for (ItemStack leftover : leftovers.values()) accepted -= leftover.getAmount();
        if (accepted <= 0) return false;
        add(machine.getInventory(), product, accepted);
        return true;
    }

    private boolean processEntity(World world, PlacedMachine machine, String input, String output) {
        org.bukkit.Location center = new org.bukkit.Location(world, machine.getX() + 0.5D, machine.getY() + 1.0D, machine.getZ() + 0.5D);
        for (Item entity : world.getEntitiesByClass(Item.class)) {
            if (entity.getLocation().distanceSquared(center) > 4.0D) continue;
            if (!input.equals(ProductItemService.readId(entity.getItemStack()))) continue;
            entity.remove();
            spawnProduct(world, machine, output);
            return true;
        }
        return false;
    }

    private boolean moveOne(FactoryProfile profile, World world, PlacedMachine source, String product) {
        Integer amount = source.getInventory().get(product);
        if (amount == null || amount <= 0) return false;
        PlacedMachine target = findOutputTarget(profile, source, product);
        if (target == null || !accept(target, world, product)) return false;
        if (amount == 1) source.getInventory().remove(product);
        else source.getInventory().put(product, amount - 1);
        return true;
    }

    private PlacedMachine findOutputTarget(FactoryProfile profile, PlacedMachine source, String product) {
        PlacedMachine fallback = null;
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            PlacedMachine target = service.findMachine(profile, source.getX() + offset[0], source.getY(), source.getZ() + offset[1]);
            if (target == null || target == source || target.getType().endsWith("_miner")) continue;
            if ("storage".equals(target.getType())) return target;
            if ("conveyor".equals(target.getType())) fallback = target;
            else if (isProcessingTarget(target, product) && fallback == null) fallback = target;
            else if (("seller".equals(target.getType())) && fallback == null) fallback = target;
        }
        return fallback;
    }

    private boolean isProcessingTarget(PlacedMachine machine, String product) {
        return ("smelter".equals(machine.getType()) && ("iron_ore".equals(product) || "ore".equals(product)))
                || ("assembler".equals(machine.getType()) && "iron_ingot".equals(product));
    }

    private boolean accept(PlacedMachine target, World world, String product) {
        if ("storage".equals(target.getType())) {
            Block block = world.getBlockAt(target.getX(), target.getY(), target.getZ());
            if (!(block.getState() instanceof org.bukkit.block.Container)) return false;
            Inventory inventory = ((org.bukkit.block.Container) block.getState()).getInventory();
            java.util.Map<Integer, ItemStack> leftovers = inventory.addItem(createProduct(product, 1));
            int accepted = 1;
            for (ItemStack leftover : leftovers.values()) accepted -= leftover.getAmount();
            if (accepted <= 0) return false;
            add(target.getInventory(), product, accepted);
            return true;
        }
        add(target.getInventory(), product, 1);
        return true;
    }

    private ItemStack createProduct(String product, int amount) {
        return ProductItemService.create(product, amount, config.getProductSellPrice(product));
    }

    private void syncStorageFromWorld(FactoryProfile profile, World world) {
        for (PlacedMachine machine : profile.getLayout()) {
            if (!"storage".equals(machine.getType())) continue;
            Block block = world.getBlockAt(machine.getX(), machine.getY(), machine.getZ());
            if (!(block.getState() instanceof org.bukkit.block.Container)) continue;
            machine.getInventory().clear();
            for (ItemStack item : ((org.bukkit.block.Container) block.getState()).getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;
                String product = ProductItemService.readId(item);
                if (product != null) add(machine.getInventory(), product, item.getAmount());
            }
        }
    }

    private void syncMachineContainers(FactoryProfile profile, World world) {
        for (PlacedMachine machine : profile.getLayout()) {
            Block block = world.getBlockAt(machine.getX(), machine.getY(), machine.getZ());
            if (!(block.getState() instanceof org.bukkit.block.Container)) continue;
            Inventory inventory = ((org.bukkit.block.Container) block.getState()).getInventory();
            inventory.clear();
            for (Map.Entry<String, Integer> entry : machine.getInventory().entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0)
                    inventory.addItem(createProduct(entry.getKey(), entry.getValue()));
            }
        }
    }

    private void syncMachineContainersFromWorld(FactoryProfile profile, World world) {
        for (PlacedMachine machine : profile.getLayout()) {
            Block block = world.getBlockAt(machine.getX(), machine.getY(), machine.getZ());
            if (!(block.getState() instanceof org.bukkit.block.Container)) continue;
            machine.getInventory().clear();
            for (ItemStack item : ((org.bukkit.block.Container) block.getState()).getInventory().getContents()) {
                String product = ProductItemService.readId(item);
                if (product != null && item.getAmount() > 0) add(machine.getInventory(), product, item.getAmount());
            }
        }
    }

    private void syncConveyorsFromWorld(FactoryProfile profile, World world) {
        for (PlacedMachine machine : profile.getLayout()) {
            if (!"conveyor".equals(machine.getType())) continue;
            Block block = world.getBlockAt(machine.getX(), machine.getY(), machine.getZ());
            if (!(block.getState() instanceof org.bukkit.block.Container)) continue;
            machine.getInventory().clear();
            for (ItemStack item : ((org.bukkit.block.Container) block.getState()).getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;
                String product = ProductItemService.readId(item);
                if (product != null) add(machine.getInventory(), product, item.getAmount());
            }
        }
    }

    private boolean convert(PlacedMachine machine, String input, String output) {
        Integer amount = machine.getInventory().get(input);
        if (amount == null || amount < 1) return false;
        machine.getInventory().put(input, amount - 1);
        add(machine.getInventory(), output, 1);
        return true;
    }

    private void add(Map<String, Integer> inventory, String id, int amount) {
        inventory.compute(id, (k, old) -> (old == null ? 0 : old) + amount);
    }

    private double getSellPrice(String product) {
        return config.getProductSellPrice(product);
    }
}