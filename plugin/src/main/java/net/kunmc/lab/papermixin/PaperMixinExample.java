package net.kunmc.lab.papermixin;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperMixinExample extends JavaPlugin {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onSneak(PlayerToggleSneakEvent event) {
                if (event.isSneaking()) {
                    System.out.println("plugin: sneak!");
                }
            }
        }, this);
    }

    @Override
    public void onDisable() {
    }
}
