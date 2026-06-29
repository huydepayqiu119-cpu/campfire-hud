package com.campfirehud;

import com.campfirehud.commands.HudCommand;
import com.campfirehud.listeners.PlayerJoinListener;
import com.campfirehud.listeners.PlayerQuitListener;
import com.campfirehud.sync.StatsSyncTask;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class CampfireHUD extends JavaPlugin {

    private static CampfireHUD instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        registerEvents(new PlayerJoinListener(), new PlayerQuitListener());
        getCommand("campfirehud").setExecutor(new HudCommand());

        int intervalTicks = getConfig().getInt("sync-interval-ticks", 10);
        Bukkit.getScheduler().runTaskTimer(this, new StatsSyncTask(), 0L, intervalTicks);

        getLogger().info("CampfireHUD enabled — syncing player stats to Bedrock clients.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CampfireHUD disabled.");
    }

    private void registerEvents(Listener... listeners) {
        Arrays.stream(listeners).forEach(l -> Bukkit.getPluginManager().registerEvents(l, this));
    }

    public static CampfireHUD getInstance() {
        return instance;
    }
}
