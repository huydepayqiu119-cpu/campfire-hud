package com.campfirehud.bridge;

import com.campfirehud.CampfireHUD;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GeyserBridge {

    private static Method sendUpstreamPacketMethod = null;
    private static boolean initialized = false;
    private static boolean available = false;

    public static boolean isBedrockPlayer(Player player) {
        return GeyserApi.api().isBedrockPlayer(player.getUniqueId());
    }

    public static void pushStats(Player player) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
        if (connection == null) return;

        if (!initialized) {
            initialized = true;
            sendUpstreamPacketMethod = findSendMethod(connection);
            available = sendUpstreamPacketMethod != null;
            if (!available) {
                CampfireHUD.getInstance().getLogger().warning(
                    "[CampfireHUD] sendUpstreamPacket not found on " + connection.getClass().getName()
                    + " — health/food bars may not update. Enable debug for details.");
            } else {
                CampfireHUD.getInstance().getLogger().info(
                    "[CampfireHUD] Found sendUpstreamPacket on " + connection.getClass().getName());
            }
        }

        if (!available) return;

        try {
            Object packet = buildUpdateAttributesPacket(player);
            if (packet == null) return;
            sendUpstreamPacketMethod.invoke(connection, packet);
        } catch (Exception e) {
            if (CampfireHUD.getInstance().getConfig().getBoolean("debug", false)) {
                CampfireHUD.getInstance().getLogger().warning(
                    "[CampfireHUD] pushStats failed for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private static Method findSendMethod(Object target) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals("sendUpstreamPacket") && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    return m;
                }
            }
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals("sendUpstreamPacket") && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        }
        if (CampfireHUD.getInstance().getConfig().getBoolean("debug", false)) {
            StringBuilder sb = new StringBuilder("[CampfireHUD] Available methods on ")
                .append(target.getClass().getName()).append(":\n");
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("send") || m.getName().toLowerCase().contains("packet")) {
                    sb.append("  ").append(m.getName()).append("(").append(m.getParameterCount()).append(" params)\n");
                }
            }
            CampfireHUD.getInstance().getLogger().info(sb.toString());
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

            List<Object> attrs = new ArrayList<>();
            attrs.add(makeAttr(attrClass, "minecraft:health",            0f, (float) maxHp, (float) currentHp, (float) maxHp));
            attrs.add(makeAttr(attrClass, "minecraft:player.hunger",     0f, 20f, food,      20f));
            attrs.add(makeAttr(attrClass, "minecraft:player.saturation", 0f, 20f, sat,       20f));
            attrs.add(makeAttr(attrClass, "minecraft:armor",             0f, 30f, (float) armor, (float) armor));

            packetClass.getMethod("setAttributes", List.class).invoke(packet, attrs);
            return packet;
        } catch (Exception e) {
            if (CampfireHUD.getInstance().getConfig().getBoolean("debug", false)) {
                CampfireHUD.getInstance().getLogger().warning(
                    "[CampfireHUD] buildPacket failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
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
