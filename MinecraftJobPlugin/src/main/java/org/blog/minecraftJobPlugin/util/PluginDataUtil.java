package com.yourname.jobplugin.util;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.UUID;
import java.util.Map;

public class PluginDataUtil {

    // 플레이어별 시스템 데이터 저장
    public static void savePlayerData(JavaPlugin plugin, UUID player, Map<String, Object> dataMap, String fileName) {
        // TODO: dataMap → yaml/json 파일로 저장
        // 예: new File(plugin.getDataFolder(), "player/" + player + "/" + fileName)
    }

    // 전체 시스템 저장
    public static void saveAll(JavaPlugin plugin) {
        // 각 매니저: jobManager.saveAll(), economyManager.saveAll() 등 호출
    }

    // 데이터 로딩
    public static Map<String, Object> loadPlayerData(JavaPlugin plugin, UUID player, String fileName) {
        // TODO: 파일 → Map 변환
        return null;
    }
}