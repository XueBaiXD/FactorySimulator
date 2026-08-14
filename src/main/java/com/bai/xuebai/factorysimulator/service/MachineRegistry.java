package com.bai.xuebai.factorysimulator.service;

import com.bai.xuebai.factorysimulator.config.PluginConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MachineRegistry {
    private final PluginConfig config;
    private final Map<String, MachineDefinition> definitions = new LinkedHashMap<String, MachineDefinition>();

    public MachineRegistry(PluginConfig config) {
        this.config = config;
        reload();
    }

    public void reload() {
        definitions.clear();
        register("basic_miner", "基础采矿机", Material.DISPENSER, 1, 1, "搭配传送带输出;连接物流仓储箱;末端连接自动售货机");
        register("coal_miner", "煤炭采矿机", Material.DROPPER, 1, 1, "搭配传送带输出;连接物流仓储箱分类存放;末端连接自动售货机", "coal");
        register("iron_miner", "铁矿采矿机", Material.DISPENSER, 1, 1, "搭配工业熔炼机;用传送带连接;熔炼后接自动装配机", "iron_ore");
        register("gold_miner", "黄金采矿机", Material.DISPENSER, 1, 1, "搭配电力熔炉;连接物流仓储箱;末端连接自动售货机", "gold_ore");
        register("redstone_miner", "红石采矿机", Material.DROPPER, 1, 1, "搭配物流仓储箱;用传送带集中运输;末端连接自动售货机", "redstone");
        register("diamond_miner", "钻石采矿机", Material.DISPENSER, 1, 1, "搭配物流仓储箱;优先连接自动售货机;可用传送带延长线路", "diamond");
        register("conveyor", "传送带", Material.HOPPER, 0, 0, "连接采矿机与加工设备;连接加工设备与仓储箱;连接仓储箱与自动售货机");
        register("smelter", "工业熔炼机", Material.FURNACE, 1, 1, "输入铁矿后输出铁锭;前端接采矿机;后端接自动装配机或仓储箱", "smelter");
        register("assembler", "自动装配机", Material.DROPPER, 1, 1, "输入铁锭后输出工厂产品;前端接工业熔炼机;后端接自动售货机", "assembler");
        register("electric_furnace", "电力熔炉", Material.FURNACE, 1, 1, "输入金矿或铁矿;前端接对应采矿机;后端接仓储箱或自动售货机", "electric_furnace");
        register("fuel_generator", "燃料发电机", Material.REDSTONE_BLOCK, 1, 1, "放在加工设备附近;搭配采矿机形成能源区;与传送带和仓储箱分区摆放");
        register("storage", "物流仓储箱", Material.CHEST, 0, 0, "作为中转缓冲;连接多条传送带;适合放在采矿区与加工区之间");
        register("seller", "自动售货机", Material.CHEST, 0, 0, "放在流水线末端;前端连接传送带或仓储箱;自动出售其中的工厂产出");
    }

    private void register(String id, String name, Material material, int input, int output, String recipe) {
        register(id, name, material, input, output, recipe, null);
    }

    private void register(String id, String name, Material material, int input, int output, String recipe, String product) {
        definitions.put(id, new MachineDefinition(id, name, material, input, output, recipe, product));
    }

    public MachineDefinition get(String id) { return definitions.get(id); }
    public Map<String, MachineDefinition> all() { return Collections.unmodifiableMap(definitions); }

    public ItemStack createItem(String id, int amount) {
        MachineDefinition definition = get(id);
        if (definition == null) return null;
        ItemStack item = new ItemStack(definition.getMaterial(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b" + definition.getName()));
        meta.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + config.getMachineMarker() + id,
                ChatColor.GRAY + "FactorySimulator 工厂设备"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public String readType(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        String marker = config.getMachineMarker();
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line);
            String expected = ChatColor.stripColor(marker);
            if (plain != null && plain.startsWith(expected)) {
                String id = plain.substring(expected.length());
                return get(id) == null ? null : id;
            }
        }
        return null;
    }

    public ItemStack createProtectedPickaxe() {
        ItemStack item = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e工厂拆卸镐"));
        meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "FS_PICKAXE", ChatColor.GRAY + "仅可拆除自己工厂内的设备"));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isProtectedPickaxe(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) if ("FS_PICKAXE".equals(ChatColor.stripColor(line))) return true;
        return false;
    }

    public ItemStack createTutorialBook() {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b工厂模拟器教程"));
        meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "FS_TUTORIAL"));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTutorialBook(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) if ("FS_TUTORIAL".equals(ChatColor.stripColor(line))) return true;
        return false;
    }

    public static class MachineDefinition {
        private final String id;
        private final String name;
        private final Material material;
        private final int input;
        private final int output;
        private final String recipe;
        private final String product;

        public MachineDefinition(String id, String name, Material material, int input, int output, String recipe, String product) {
            this.id = id; this.name = name; this.material = material; this.input = input; this.output = output; this.recipe = recipe; this.product = product;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public Material getMaterial() { return material; }
        public int getInput() { return input; }
        public int getOutput() { return output; }
        public String getRecipe() { return recipe; }
        public String getProduct() { return product; }
    }
}