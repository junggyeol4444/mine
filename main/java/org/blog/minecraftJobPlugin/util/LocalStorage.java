package org.blog.minecraftJobPlugin.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LocalStorage {

    private final JavaPlugin plugin;
    private final File dataDir;
    private final File playerDir;
    private final File configDir;

    public LocalStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "data");
        this.playerDir = new File(dataDir, "player");
        this.configDir = new File(plugin.getDataFolder(), "config");
        if (!dataDir.exists()) dataDir.mkdirs();
        if (!playerDir.exists()) playerDir.mkdirs();
        if (!configDir.exists()) configDir.mkdirs();
    }

    public YamlConfiguration loadPlayerConfig(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            YamlConfiguration cfg = new YamlConfiguration();
            try { cfg.save(file); } catch (IOException ignored) {}
            return cfg;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void savePlayerConfigSync(UUID uuid, YamlConfiguration cfg) {
        File file = getPlayerFile(uuid);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("플레이어 파일 저장 실패: " + file.getName() + " - " + e.getMessage());
        }
    }

    public File getPlayerFile(UUID uuid) {
        return new File(playerDir, uuid.toString() + ".yml");
    }

    public YamlConfiguration loadGlobalConfig(String name) {
        File file = new File(configDir, name + ".yml");
        if (file.exists()) return YamlConfiguration.loadConfiguration(file);

        // try resource copy
        try (InputStream is = plugin.getResource("config/" + name + ".yml")) {
            if (is != null) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
                try { cfg.save(file); } catch (IOException ignored) {}
                return cfg;
            }
        } catch (Exception ignored) {}
        return new YamlConfiguration();
    }

    public void saveGlobalConfigSync(String name, YamlConfiguration cfg) {
        File file = new File(configDir, name + ".yml");
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("글로벌 파일 저장 실패: " + file.getName() + " - " + e.getMessage());
        }
    }
}