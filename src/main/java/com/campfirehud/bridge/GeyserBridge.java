package com.campfirehud.bridge;

import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;

public class GeyserBridge {

    public static boolean isBedrockPlayer(Player player) {
        return GeyserApi.api().isBedrockPlayer(player.getUniqueId());
    }
}
