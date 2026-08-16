package com.bai.xuebai.factorysimulator.listener;

import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.model.PlacedMachine;
import com.bai.xuebai.factorysimulator.service.FactoryService;
import com.bai.xuebai.factorysimulator.service.MachineRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class FactoryBlockListener implements Listener {
    private final FactoryService service;
    private final MachineRegistry registry;

    public FactoryBlockListener(FactoryService service, MachineRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        String type = registry.readType(event.getItemInHand());
        if (type == null) return;
        Player player = event.getPlayer();
        FactoryProfile profile = service.getOrCreate(player);
        if (!profile.getWorldName().equals(event.getBlock().getWorld().getName())) {
            event.setCancelled(true);
            return;
        }
        Location location = event.getBlock().getLocation();
        if (!service.isInsidePlot(profile, location.getBlockX(), location.getBlockZ())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "只能在自己的工厂地皮内放置设备。");
            return;
        }
        profile.getLayout().add(new PlacedMachine(type, location.getBlockX(), location.getBlockY(), location.getBlockZ(), event.getBlockAgainst().getFace(event.getBlock()).name()));
        service.save(profile);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        FactoryProfile profile = service.getByWorld(event.getBlock().getWorld().getName());
        if (profile == null) return;
        if (!profile.getPlayerId().equals(event.getPlayer().getUniqueId().toString())) {
            event.setCancelled(true);
            return;
        }
        PlacedMachine machine = service.findMachine(profile, event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ());
        if (machine == null) return;
        if (!registry.isProtectedPickaxe(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "只有工厂拆卸镐可以拆除设备。");
            return;
        }
        event.setCancelled(true);
        event.getBlock().setType(org.bukkit.Material.AIR);
        remove(profile, machine);
        ItemStack item = registry.createItem(machine.getType(), 1);
        if (item != null) event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
        service.save(profile);
    }

    private void remove(FactoryProfile profile, PlacedMachine target) {
        Iterator<PlacedMachine> iterator = profile.getLayout().iterator();
        while (iterator.hasNext()) if (iterator.next() == target) {
            iterator.remove();
            return;
        }
    }
}