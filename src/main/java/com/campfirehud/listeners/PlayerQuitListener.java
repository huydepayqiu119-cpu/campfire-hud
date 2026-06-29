package com.campfirehud.listeners;

import com.campfirehud.bridge.PlaceholderBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlaceholderBridge.cleanup(event.getPlayer());
    }
}
