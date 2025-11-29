package org.blog.minecraftJobPlugin.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigUtil {
    private static final Map<String, YamlConfiguration> CACHE = new HashMap<>();
    private static JavaPlugin pluginInstance;  // 추가

    public static void loadAllConfigs(JavaPlugin plugin) {  // 파라미터 추가
        pluginInstance = plugin;  // 저장
        String[] names = new String[] {"jobs","quests","skills","traits","shops","grades","combos","economy"};
        for (String n : names) getConfig(plugin, n);
    }

    public static YamlConfiguration getConfig(String name) {
        return getConfig(pluginInstance, name);  // pluginInstance 사용
    }

    public static YamlConfiguration getConfig(JavaPlugin plugin, String name) {
        if (CACHE.containsKey(name)) return CACHE.get(name);

        PluginDataUtil dataUtil = new PluginDataUtil(plugin);  // LocalStorage → PluginDataUtil
        YamlConfiguration cfg = dataUtil.loadGlobal(name);

        if ((cfg == null || cfg.getKeys(false).isEmpty())) {
            try (InputStream is = plugin.getResource("config/" + name + ".yml")) {
                if (is != null) {
                    cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
                    try {
                        java.io.File file = new java.io.File(plugin.getDataFolder(), "config/" + name + ".yml");
                        file.getParentFile().mkdirs();
                        cfg.save(file);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        if (cfg == null) cfg = new YamlConfiguration();
        CACHE.put(name, cfg);
        return cfg;
    }
}