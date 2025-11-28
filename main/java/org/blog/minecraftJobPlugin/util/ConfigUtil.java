package org.blog.minecraftJobPlugin.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * resources/config/*.yml 을 data/config/*.yml 로 복사하고 캐시 제공
 */
public class ConfigUtil {
    private static final Map<String, YamlConfiguration> CACHE = new HashMap<>();

    public static void loadAllConfigs(JavaPlugin plugin) {
        String[] names = new String[] {"jobs","quests","skills","traits","shops","grades","combos","economy"};
        for (String n : names) getConfig(plugin, n);
    }

    public static YamlConfiguration getConfig(String name) {
        return CACHE.get(name);
    }

    public static YamlConfiguration getConfig(JavaPlugin plugin, String name) {
        if (CACHE.containsKey(name)) return CACHE.get(name);
        LocalStorage ls = new LocalStorage(plugin);
        YamlConfiguration cfg = ls.loadGlobalConfig(name);
        // fallback to resource if file empty
        if ((cfg == null || cfg.getKeys(false).isEmpty())) {
            try (InputStream is = plugin.getResource("config/" + name + ".yml")) {
                if (is != null) {
                    cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
                    try { cfg.save(new File(plugin.getDataFolder(), "config/" + name + ".yml")); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        if (cfg == null) cfg = new YamlConfiguration();
        CACHE.put(name, cfg);
        return cfg;
    }
}