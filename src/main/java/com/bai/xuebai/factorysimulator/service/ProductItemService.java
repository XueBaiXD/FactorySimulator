package com.bai.xuebai.factorysimulator.service;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class ProductItemService {
    private static final String MARKER = "§0FS_PRODUCT:";

    private ProductItemService() {
    }

    public static ItemStack create(String id, int amount, double price) {
        Product product = product(id);
        if (product == null) return null;
        ItemStack item = new ItemStack(product.material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f" + product.name));
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "回收价格: " + ChatColor.GOLD + String.format("%.2f", price),
                ChatColor.DARK_GRAY + MARKER + id
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static String readId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line);
            if (plain != null && plain.startsWith("FS_PRODUCT:")) return plain.substring("FS_PRODUCT:".length());
        }
        return null;
    }

    public static String getName(String id) {
        Product product = product(id);
        return product == null ? id : product.name;
    }

    public static Material getMaterial(String id) {
        Product product = product(id);
        return product == null ? Material.IRON_ORE : product.material;
    }

    private static Product product(String id) {
        if ("iron_ore".equals(id) || "ore".equals(id)) return new Product("铁矿石", Material.IRON_ORE);
        if ("coal".equals(id)) return new Product("煤炭", Material.COAL);
        if ("iron_ingot".equals(id)) return new Product("铁锭", Material.IRON_INGOT);
        if ("gold_ore".equals(id)) return new Product("金矿石", Material.GOLD_ORE);
        if ("gold_ingot".equals(id)) return new Product("金锭", Material.GOLD_INGOT);
        if ("redstone".equals(id)) return new Product("红石", Material.REDSTONE);
        if ("diamond".equals(id)) return new Product("钻石", Material.DIAMOND);
        if ("factory_product".equals(id)) return new Product("工业组件", Material.IRON_BLOCK);
        if ("advanced_component".equals(id)) return new Product("高级组件", Material.GOLD_BLOCK);
        return null;
    }

    private static class Product {
        private final String name;
        private final Material material;

        private Product(String name, Material material) {
            this.name = name;
            this.material = material;
        }
    }
}