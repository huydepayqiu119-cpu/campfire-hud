package com.campfirehud.bridge;

import com.campfirehud.CampfireHUD;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.ArrayList;
import java.util.List;

public class GeyserBridge {

    private static final String ATTR_HEALTH     = "minecraft:health";
    private static final String ATTR_HUNGER     = "minecraft:player.hunger";
    private static final String ATTR_SATURATION = "minecraft:player.saturation";
    private static final String ATTR_ARMOR      = "minecraft:armor";

    public static void pushStats(Player player) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
        if (connection == null) return;

        double maxHp      = getMaxHealth(player);
        double currentHp  = Math.max(0, player.getHealth());
        int    food       = player.getFoodLevel();
        float  saturation = player.getSaturation();
        double armor      = Math.min(getArmorValue(player), 20.0);

        List<AttributeData> attributes = new ArrayList<>();
        attributes.add(new AttributeData(ATTR_HEALTH,     0f, (float) maxHp, (float) currentHp, (float) maxHp));
        attributes.add(new AttributeData(ATTR_HUNGER,     0f, 20f, food, 20f));
        attributes.add(new AttributeData(ATTR_SATURATION, 0f, 20f, saturation, 20f));
        attributes.add(new AttributeData(ATTR_ARMOR,      0f, 30f, (float) armor, (float) armor));

        UpdateAttributesPacket packet = new UpdateAttributesPacket();
        packet.setRuntimeEntityId(1L);
        packet.setAttributes(attributes);
        packet.setTick(0L);

        try {
            connection.sendUpstreamPacket(packet);
        } catch (Exception e) {
            if (CampfireHUD.getInstance().getConfig().getBoolean("debug", false)) {
                CampfireHUD.getInstance().getLogger().warning(
                        "Failed to push stats for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private static double getMaxHealth(Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    private static double getArmorValue(Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_ARMOR);
        return attr != null ? attr.getValue() : 0.0;
    }
}
