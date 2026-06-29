package com.campfirehud.sync;

import com.campfirehud.bridge.GeyserBridge;
import com.campfirehud.bridge.PlaceholderBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;

public class StatsSyncTask implements Runnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!GeyserApi.api().isBedrockPlayer(player.getUniqueId())) continue;
            GeyserBridge.pushStats(player);
            PlaceholderBridge.push(player);
        }
    }
}
