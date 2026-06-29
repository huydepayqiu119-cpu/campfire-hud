package com.campfirehud.bridge;

import com.campfirehud.CampfireHUD;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class PlaceholderBridge {

    private static final String OBJ_PREFIX = "chud_";

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public static void push(Player player) {
        if (!isAvailable()) return;

        var section = CampfireHUD.getInstance().getConfig()
                .getConfigurationSection("placeholders");
        if (section == null) return;

        Scoreboard board = getOrCreateBoard(player);

        for (String key : section.getKeys(false)) {
            String placeholder = section.getString(key, "");
            String resolved = PlaceholderAPI.setPlaceholders(player, placeholder);
            int value = parseIntSafe(resolved);

            String objName = OBJ_PREFIX + key;
            if (objName.length() > 16) objName = objName.substring(0, 16);

            Objective obj = board.getObjective(objName);
            if (obj == null) {
                obj = board.registerNewObjective(objName, Criteria.DUMMY, key);
            }
            obj.getScore(player.getName()).setScore(value);
        }
    }

    public static void cleanup(Player player) {
        var manager = Bukkit.getScoreboardManager();
        Scoreboard board = player.getScoreboard();
        if (board == manager.getMainScoreboard()) return;
        board.getObjectives().stream()
                .filter(o -> o.getName().startsWith(OBJ_PREFIX))
                .forEach(Objective::unregister);
    }

    private static Scoreboard getOrCreateBoard(Player player) {
        var manager = Bukkit.getScoreboardManager();
        Scoreboard board = player.getScoreboard();
        if (board == manager.getMainScoreboard()) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }
        return board;
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            String cleaned = s.replaceAll("[^\\d.-]", "");
            if (cleaned.isEmpty()) return 0;
            return (int) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
