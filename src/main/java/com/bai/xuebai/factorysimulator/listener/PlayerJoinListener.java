package com.bai.xuebai.factorysimulator.listener;

import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.service.FactoryService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final FactoryService service;

    public PlayerJoinListener(FactoryService service) {
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        FactoryProfile profile = service.join(event.getPlayer());
        if (!profile.isCreated()) event.getPlayer().sendMessage("§e使用 /fs create 创建你的专属工厂。");
        else event.getPlayer().sendMessage("§a使用 /fs menu 打开工厂教程。");
    }
}