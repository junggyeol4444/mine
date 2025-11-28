package org.blog.minecraftJobPlugin.job;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.UUID;

public class JobGradeManager {
    private final JobPlugin plugin;
    private final PluginDataUtil dataUtil;
    private final Map<UUID, Map<String, String>> playerGrades = new HashMap<>();

    public JobGradeManager(JobPlugin plugin) {
        this.plugin = plugin;
        this.dataUtil = new PluginDataUtil(plugin);
        loadGrades();
    }

    public String getGrade(Player p, String job) {
        return playerGrades.getOrDefault(p.getUniqueId(), Collections.emptyMap()).getOrDefault(job, "D");
    }

    public void upgradeGrade(Player p, String job, String grade) {
        playerGrades.computeIfAbsent(p.getUniqueId(), k->new HashMap<>()).put(job, grade);
        savePlayerGradesAsync(p.getUniqueId());
        p.sendMessage("§d" + job + " 등급이 " + grade + "로 승급되었습니다!");
    }

    private void loadGrades() {
        java.io.File playerDir = new java.io.File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) return;
        java.io.File[] files = playerDir.listFiles((d,n)->n.endsWith(".yml"));
        if (files == null) return;
        for (java.io.File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml",""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                Map<String, String> grades = new HashMap<>();
                if (cfg.isConfigurationSection("grades")) {
                    for (String job : cfg.getConfigurationSection("grades").getKeys(false)) {
                        grades.put(job, cfg.getString("grades." + job));
                    }
                }
                if (!grades.isEmpty()) playerGrades.put(uuid, grades);
            } catch (Exception ignored) {}
        }
    }

    private void savePlayerGradesAsync(UUID uuid) {
        YamlConfiguration cfg = dataUtil.loadPlayerConfig(uuid);
        Map<String, String> grades = playerGrades.getOrDefault(uuid, Collections.emptyMap());
        for (Map.Entry<String,String> e : grades.entrySet()) cfg.set("grades." + e.getKey(), e.getValue());
        dataUtil.savePlayerConfigAsync(uuid, cfg);
    }

    public void saveAllAsync() {
        for (UUID u : playerGrades.keySet()) savePlayerGradesAsync(u);
    }

    public void saveAll() {
        for (UUID u : playerGrades.keySet()) {
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(u);
            Map<String,String> grades = playerGrades.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String,String> e : grades.entrySet()) cfg.set("grades." + e.getKey(), e.getValue());
            dataUtil.savePlayerConfigSync(u, cfg);
        }
    }
}