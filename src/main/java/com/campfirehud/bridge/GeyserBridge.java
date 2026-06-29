package com.campfirehud.bridge;

import com.campfirehud.CampfireHUD;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.lang.reflect.Method;

public class GeyserBridge {

    private static Method sendUpstreamPacketMethod = null;
    private static boolean reflectionFailed = false;

    public static boolean isBedrockPlayer(Player player) {
        return GeyserApi.api().isBedrockPlayer(player.getUniqueId());
    }

    public static void pushStats(Player player) {
        if (reflectionFailed) return;

        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
        if (connection == null) return;

        try {
            Object packet = buildUpdateAttributesPacket(player);
            if (packet == null) return;

            if (sendUpstreamPacketMethod == null) {
                sendUpstreamPacketMethod = findSendMethod(connection);
            }
            if (sendUpstreamPacketMethod != null) {
                sendUpstreamPacketMethod.invoke(connection, packet);
            }
        } catch (Exception e) {
            reflectionFailed = true;
            if (CampfireHUD.getInstance().getConfig().getBoolean("debug", false)) {
                CampfireHUD.getInstance().getLogger().warning("GeyserBridge reflection failed: " + e.getMessage()
                    + " — stats will rely on Geyser's built-in sync.");
            }
        }
    }

    private static Method findSendMethod(GeyserConnection connection) {
        for (Method m : connection.getClass().getMethods()) {
            if (m.getName().equals("sendUpstreamPacket") && m.getParameterCount() == 1) {
                m.setAccessible(true);
                return m;
            }
        }
        for (Method m : connection.getClass().getDeclaredMethods()) {
            if (m.getName().equals("sendUpstreamPacket") && m.getParameterCount() == 1) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static Object buildUpdateAttributesPacket(Player player) {
        try {
            Class<?> packetClass = Class.forName(
                "org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket");
            Class<?> attrClass = Class.forName(
                "org.cloudburstmc.protocol.bedrock.data.AttributeData");

            Object packet = packetClass.getDeclaredConstructor().newInstance();
            packetClass.getMethod("setRuntimeEntityId", long.class).invoke(packet, 1L);
            packetClass.getMethod("setTick", long.class).invoke(packet, 0L);

            double maxHp    = getMaxHealth(player);
            double currentHp = Math.max(0, player.getHealth());
            int    food     = player.getFoodLevel();
            float  sat      = player.getSaturation();
            double armor    = Math.min(getArmorValue(player), 20.0);

            java.util.List<Object> attrs = new java.util.ArrayList<>();
            attrs.add(makeAttr(attrClass, "minecraft:health",          0f, (float) maxHp,  (float) currentHp, (float) maxHp));
            attrs.add(makeAttr(attrClass, "minecraft:player.hunger",   0f, 20f,             food,              20f));
            attrs.add(makeAttr(attrClass, "minecraft:player.saturation", 0f, 20f,           sat,               20f));
            attrs.add(makeAttr(attrClass, "minecraft:armor",           0f, 30f,             (float) armor,     (float) armor));

            packetClass.getMethod("setAttributes", java.util.List.class).invoke(packet, attrs);
            return packet;
        } catch (Exception e) {
            return null;
        }
    }

    private static Object makeAttr(Class<?> attrClass, String id,
                                   float min, float max, float value, float def) throws Exception {
        return attrClass.getDeclaredConstructor(String.class, float.class, float.class, float.class, float.class)
                .newInstance(id, min, max, value, def);
    }

    private static double getMaxHealth(Player player) {
        var a = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return a != null ? a.getValue() : 20.0;
    }

    private static double getArmorValue(Player player) {
        var a = player.getAttribute(Attribute.GENERIC_ARMOR);
        return a != null ? a.getValue() : 0.0;
    }
}
