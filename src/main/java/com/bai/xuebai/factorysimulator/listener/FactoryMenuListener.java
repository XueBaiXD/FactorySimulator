package com.bai.xuebai.factorysimulator.listener;

import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.service.FactoryService;
import com.bai.xuebai.factorysimulator.service.MachineRegistry;
import com.bai.xuebai.factorysimulator.service.ProductItemService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FactoryMenuListener implements Listener {
    private final FactorySimulator plugin;
    private final FactoryService service;
    private final MachineRegistry registry;
    public FactoryMenuListener(FactorySimulator plugin, FactoryService service, MachineRegistry registry) { this.plugin = plugin; this.service = service; this.registry = registry; }

    @EventHandler public void onBook(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && registry.isTutorialBook(event.getItem())) {
            event.setCancelled(true); open(event.getPlayer());
        }
    }
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8工厂模拟器");
        inv.setItem(11, button(Material.BOOK, "§d机器搭配指南", "§7查看设备与传送带、仓储、出售设备的搭配"));
        inv.setItem(13, button(Material.EMERALD, "§a设备商店", "§7使用工厂资金购买设备"));
        inv.setItem(15, button(Material.HOPPER, "§6出售", "§7出售带有工厂标识的产出物品"));
        player.openInventory(inv);
    }
    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!isFactoryMenu(event.getView().getTitle())) return;
        if ("§8工厂出售".equals(event.getView().getTitle())) {
            if (event.getRawSlot() == 22) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) collect((Player) event.getWhoClicked(), event.getInventory());
            } else {
                event.setCancelled(false);
            }
            return;
        }
        event.setCancelled(true); if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked(); ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if ("设备商店".equals(name)) shop(player);
        else if ("机器搭配指南".equals(name)) recipes(player);
        else if ("出售".equals(name)) recycle(player);
        else if (name.startsWith("购买 ")) buy(player, item);
    }
    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (!isFactoryMenu(event.getView().getTitle())) return;
        if (!"§8工厂出售".equals(event.getView().getTitle())) {
            event.setCancelled(true);
            return;
        }
        for (Integer slot : event.getRawSlots()) {
            if (slot == 22) {
                event.setCancelled(true);
                return;
            }
        }
    }
    private boolean isFactoryMenu(String title) {
        return "§8工厂模拟器".equals(title) || "§8设备商店".equals(title) || "§8机器搭配指南".equals(title) || "§8工厂出售".equals(title);
    }

    private void recycle(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8工厂出售");
        inv.setItem(22, button(Material.EMERALD, "§a点击出售", "§7将上方产出物品出售并结算资金"));
        player.openInventory(inv);
    }

    @EventHandler public void onClose(InventoryCloseEvent event) {
        if (!"§8工厂出售".equals(event.getView().getTitle())) return;
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == 22) continue;
            ItemStack item = inventory.getItem(slot);
            if (item != null) player.getInventory().addItem(item);
        }
    }

    private void shop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8设备商店"); int slot = 10;
        for (MachineRegistry.MachineDefinition definition : registry.all().values()) {
            java.util.List<String> lore = new java.util.ArrayList<String>();
            lore.add("§7设备：§f" + definition.getName());
            lore.add("§7价格：§e" + plugin.getPluginConfig().getMachinePrice(definition.getId()));
            lore.add("§7解锁等级：§f" + plugin.getPluginConfig().getMachineUnlockLevel(definition.getId()));
            lore.add("§8FS_SHOP:" + definition.getId());
            inv.setItem(slot++, button(definition.getMaterial(), "§a购买 " + definition.getName(), lore));
        }
        player.openInventory(inv);
    }

    private void recipes(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8机器搭配指南");
        int slot = 0;
        for (MachineRegistry.MachineDefinition definition : registry.all().values()) {
            if (slot >= inv.getSize()) break;
            String[] combinations = definition.getRecipe().split(";");
            java.util.List<String> lore = new java.util.ArrayList<String>();
            lore.add("§7设备 ID: §f" + definition.getId());
            lore.add("§7解锁等级: §f" + plugin.getPluginConfig().getMachineUnlockLevel(definition.getId()));
            lore.add("§e推荐搭配:");
            for (String combination : combinations) lore.add("§f  " + combination);
            lore.add("§7设备由商店直接购买，无需生存材料");
            inv.setItem(slot++, button(definition.getMaterial(), "§d" + definition.getName(), lore));
        }
        player.openInventory(inv);
    }
    private void buy(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line);
            if (plain != null && plain.startsWith("FS_SHOP:")) {
                player.closeInventory();
                player.performCommand("fs buy " + plain.substring("FS_SHOP:".length()));
                return;
            }
        }
    }

    private void collect(Player player, Inventory inventory) {
        double total = 0D;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == 22) continue;
            ItemStack item = inventory.getItem(slot);
            String id = ProductItemService.readId(item);
            if (id == null) continue;
            total += plugin.getPluginConfig().getProductSellPrice(id) * item.getAmount();
            inventory.setItem(slot, null);
        }
        FactoryProfile profile = service.getOrCreate(player);
        profile.setMoney(profile.getMoney() + total);
        service.save(profile);
        player.sendMessage("§a出售完成，获得 §e" + String.format("%.2f", total) + " §a金币。");
    }
    private ItemStack button(Material material, String name, String lore) { return button(material, name, java.util.Collections.singletonList(lore)); }
    private ItemStack button(Material material, String name, java.util.List<String> lore) { ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); meta.setLore(lore); item.setItemMeta(meta); return item; }
}