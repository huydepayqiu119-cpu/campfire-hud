package com.campfirehud.commands;

import com.campfirehud.CampfireHUD;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HudCommand implements CommandExecutor, TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("campfirehud.admin")) {
            sender.sendMessage(CampfireHUD.getInstance().getConfig().getString(
                    "messages.no-permission", "§cNo permission."));
            return true;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("reload")
                || args[0].equalsIgnoreCase("rl"))) {
            CampfireHUD.getInstance().reloadConfig();
            sender.sendMessage(CampfireHUD.getInstance().getConfig().getString(
                    "messages.reload-success", "§aCampfireHUD reloaded."));
            return true;
        }
        sender.sendMessage("§eUsage: /campfirehud reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}
