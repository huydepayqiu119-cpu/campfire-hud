package com.campfirehud.listeners;

import com.campfirehud.bridge.GeyserBridge;
import com.campfirehud.bridge.PlaceholderBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (!GeyserBridge.isBedrockPlayer(player)) return;
        PlaceholderBridge.push(player);
    }
}
