package com.campfirehud.listeners;

import com.campfirehud.bridge.GeyserBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.geyser.api.GeyserApi;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (!GeyserApi.api().isBedrockPlayer(player.getUniqueId())) return;
        GeyserBridge.pushStats(player);
    }
}
