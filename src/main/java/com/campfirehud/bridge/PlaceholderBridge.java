package com.campfirehud.bridge;

import com.campfirehud.CampfireHUD;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
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

    /**
     * Resolves all placeholders defined in config and pushes them to Bedrock via scoreboard.
     * Numeric placeholders → scoreboard score (readable as #score_* or via score_sidebar)
     * Text placeholders → objective display name (readable as scoreboard title)
     */
    public static void push(Player player) {
        if (!isAvailable()) return;

        var section = CampfireHUD.getInstance().getConfig()
                .getConfigurationSection("placeholders");
        if (section == null) return;

        Scoreboard board = getOrCreateBoard(player);

        for (String key : section.getKeys(false)) {
            String placeholder = section.getString(key, "");
            String resolved = PlaceholderAPI.setPlaceholders(player, placeholder);
            int numValue = parseIntSafe(resolved);

            boolean debug = CampfireHUD.getInstance().getConfig().getBoolean("debug", false);
            if (debug) {
                CampfireHUD.getInstance().getLogger().info(
                    "[CampfireHUD] PAPI " + key + ": '" + placeholder + "' → '" + resolved + "' (score=" + numValue + ")");
            }

            String objName = OBJ_PREFIX + key;
            if (objName.length() > 16) objName = objName.substring(0, 16);

            Objective obj = board.getObjective(objName);
            if (obj == null) {
                // Use resolved text as display name so JSON UI can read it via objective title
                obj = board.registerNewObjective(objName, Criteria.DUMMY,
                        Component.text(resolved));
            } else {
                // Update display name with new resolved value (for text display in JSON UI)
                obj.displayName(Component.text(resolved));
            }
            // Also set as score so numeric values work
            obj.getScore(player.getName()).setScore(numValue);
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

    /**
     * Strips non-numeric chars and parses to int.
     * "1,234.56" → 1234, "20" → 20, "hello" → 0
     */
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
