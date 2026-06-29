package com.campfirehud.sync;

import com.campfirehud.bridge.GeyserBridge;
import com.campfirehud.bridge.PlaceholderBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StatsSyncTask implements Runnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!GeyserBridge.isBedrockPlayer(player)) continue;
            PlaceholderBridge.push(player);
        }
    }
}
