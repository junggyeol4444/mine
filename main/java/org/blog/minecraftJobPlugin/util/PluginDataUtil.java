package org.blog.minecraftJobPlugin.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * LocalStorage 래퍼. 비동기 저장 유틸 제공
 */
public class PluginDataUtil {

    private final LocalStorage storage;
    private final JavaPlugin plugin;

    public PluginDataUtil(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storage = new LocalStorage(plugin);
    }

    public YamlConfiguration loadPlayerConfig(UUID player) {
        return storage.loadPlayerConfig(player);
    }

    public void savePlayerConfigAsync(UUID player, YamlConfiguration cfg) {
        new BukkitRunnable() {
            @Override
            public void run() {
                storage.savePlayerConfigSync(player, cfg);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void savePlayerConfigSync(UUID player, YamlConfiguration cfg) {
        storage.savePlayerConfigSync(player, cfg);
    }

    public YamlConfiguration loadGlobal(String filename) {
        return storage.loadGlobalConfig(filename);
    }

    public void saveGlobalAsync(String filename, YamlConfiguration cfg) {
        new BukkitRunnable() {
            @Override
            public void run() {
                storage.saveGlobalConfigSync(filename, cfg);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void saveGlobalSync(String filename, YamlConfiguration cfg) {
        storage.saveGlobalConfigSync(filename, cfg);
    }
}