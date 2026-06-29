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
        }

        if (!available) return;

        boolean debug = CampfireHUD.getInstance().getConfig().getBoolean("debug", false);

        try {
            Object attrsPacket = buildUpdateAttributesPacket(player);
            if (attrsPacket != null) {
                sendUpstreamPacketMethod.invoke(connection, attrsPacket);
                if (debug) {
                    CampfireHUD.getInstance().getLogger().info(
                        "[CampfireHUD] Pushed stats for " + player.getName()
                        + " | HP=" + String.format("%.1f", player.getHealth())
                        + "/" + String.format("%.1f", getMaxHealth(player))
                        + " food=" + player.getFoodLevel()
                        + " armor=" + (int) getArmorValue(player)
                        + " air=" + player.getRemainingAir() + "/" + player.getMaximumAir()
                    );
                }
            }

            sendScorePackets(player, connection);

        } catch (Exception e) {
            if (CampfireHUD.getInstance().getConfig().getBoolean("debug", false)) {
                CampfireHUD.getInstance().getLogger().warning(
                    "[CampfireHUD] pushStats failed for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void sendScorePackets(Player player, GeyserConnection connection) throws Exception {
        Class<?> packetClass = Class.forName(
            "org.cloudburstmc.protocol.bedrock.packet.SetScorePacket");
        Class<?> entryClass = Class.forName(
            "org.cloudburstmc.protocol.bedrock.data.ScoreInfo");
        Class<?> actionEnum = Class.forName(
            "org.cloudburstmc.protocol.bedrock.packet.SetScorePacket$Action");

        Object setAction = null;
        for (Object o : actionEnum.getEnumConstants()) {
            if (o.toString().equals("SET")) { setAction = o; break; }
        }
        if (setAction == null) return;

        int health  = (int) Math.ceil(player.getHealth());
        int maxHp   = (int) Math.ceil(getMaxHealth(player));
        int food    = player.getFoodLevel();
        int armor   = (int) getArmorValue(player);
        int air     = player.getRemainingAir();
        int maxAir  = player.getMaximumAir();
        int airPct  = maxAir > 0 ? (air * 20 / maxAir) : 20;

        String[][] scores = {
            {"chud_health", String.valueOf(health)},
            {"chud_maxhp",  String.valueOf(maxHp)},
            {"chud_food",   String.valueOf(food)},
            {"chud_armor",  String.valueOf(armor)},
            {"chud_air",    String.valueOf(airPct)}
        };

        List<Object> entries = new ArrayList<>();
        long scoreId = 1000;

        for (String[] s : scores) {
            try {
                Object entry = entryClass.getDeclaredConstructor(
                    long.class, String.class, int.class, String.class)
                    .newInstance(scoreId++, s[0], Integer.parseInt(s[1]), player.getName());
                entries.add(entry);
            } catch (Exception ignored) {
                // Constructor signature may differ by version
            }
        }

        if (entries.isEmpty()) return;

        Object packet = packetClass.getDeclaredConstructor().newInstance();
        packetClass.getMethod("setAction", actionEnum).invoke(packet, setAction);
        packetClass.getMethod("getInfos").invoke(packet);
        Method getInfos = packetClass.getMethod("getInfos");
        ((List) getInfos.invoke(packet)).addAll(entries);

        sendUpstreamPacketMethod.invoke(connection, packet);
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
            clazz = clazz.getSuperclass();
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

            double maxHp     = getMaxHealth(player);
            double currentHp = Math.max(0, player.getHealth());
            int    food      = player.getFoodLevel();
            float  sat       = player.getSaturation();
            double armor     = Math.min(getArmorValue(player), 20.0);
            int    air       = player.getRemainingAir();
            int    maxAir    = player.getMaximumAir();
            float  airScaled = maxAir > 0 ? ((float) air / maxAir) * 300f : 300f;
            float  xpProgress = player.getExp();
            int    xpLevel   = player.getLevel();
            float  xpTotal   = player.getTotalExperience();

            List<Object> attrs = new ArrayList<>();
            attrs.add(makeAttr(attrClass, "minecraft:health",            0f, (float) maxHp, (float) currentHp, (float) maxHp));
            attrs.add(makeAttr(attrClass, "minecraft:player.hunger",     0f, 20f, food, 20f));
            attrs.add(makeAttr(attrClass, "minecraft:player.saturation", 0f, 20f, sat,  20f));
            attrs.add(makeAttr(attrClass, "minecraft:armor",             0f, 30f, (float) armor, (float) armor));
            attrs.add(makeAttr(attrClass, "minecraft:player.exhaustion", 0f, 300f, airScaled, 300f));
            attrs.add(makeAttr(attrClass, "minecraft:player.experience", 0f, 1f, xpProgress, 0f));
            attrs.add(makeAttr(attrClass, "minecraft:player.level",      0f, 24791.0f, xpLevel, 0f));

            packetClass.getMethod("setAttributes", List.class).invoke(packet, attrs);
            return packet;
        } catch (Exception e) {
            if (CampfireHUD.getInstance().getConfig().getBoolean("debug", false)) {
                CampfireHUD.getInstance().getLogger().warning(
                    "[CampfireHUD] buildPacket: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            return null;
        }
    }

    private static Object makeAttr(Class<?> c, String id,
                                    float min, float max, float val, float def) throws Exception {
        return c.getDeclaredConstructor(String.class, float.class, float.class, float.class, float.class)
                .newInstance(id, min, max, val, def);
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
