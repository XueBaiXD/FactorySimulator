package com.bai.xuebai.factorysimulator.listener;

import com.bai.xuebai.factorysimulator.service.FactoryService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class FactoryWorldListener implements Listener {
    private final FactoryService service;

    public FactoryWorldListener(FactoryService service) {
        this.service = service;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (service.getByWorld(event.getLocation().getWorld().getName()) != null) {
            event.setCancelled(true);
        }
    }
}