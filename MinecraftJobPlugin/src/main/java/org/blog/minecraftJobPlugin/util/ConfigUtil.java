package com.yourname.jobplugin.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 모든 YAML 설정 데이터 로딩 및 전역 접근 유틸
 */
public class ConfigUtil {
    public static Map<String, YamlConfiguration> configs = new HashMap<>();

    public static void loadAllConfigs(JavaPlugin plugin) {
        String[] configNames = { "jobs", "shops", "quests", "skills", "traits", "grades", "combos", "economy" };
        File configDir = new File(plugin.getDataFolder(), "config");
        if (!configDir.exists()) configDir.mkdirs();

        for (String name : configNames) {
            File file = new File(configDir, name + ".yml");
            if (!file.exists()) plugin.saveResource("config/" + name + ".yml", false);
            configs.put(name, YamlConfiguration.loadConfiguration(file));
        }
    }

    public static YamlConfiguration getConfig(String name) {
        return configs.get(name);
    }

    // 개별 저장 (예: 시스템 데이터 변경 → 파일로 반영)
    public static void saveConfig(String name, JavaPlugin plugin) {
        YamlConfiguration config = configs.get(name);
        if (config == null) return;
        File file = new File(plugin.getDataFolder(), "config/" + name + ".yml");
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("설정 파일 저장 오류: " + file.getName());
        }
    }
}