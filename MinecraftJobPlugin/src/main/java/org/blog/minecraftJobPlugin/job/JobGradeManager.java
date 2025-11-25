package com.yourname.jobplugin.job;

import com.yourname.jobplugin.JobPlugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class JobGradeManager {
    private final JobPlugin plugin;
    // [플레이어 UUID] → [직업명] → 등급
    private final Map<UUID, Map<String, String>> playerGrades = new HashMap<>();

    public JobGradeManager(JobPlugin plugin) {
        this.plugin = plugin;
        loadGrades();
    }

    // 플레이어 등급 조회
    public String getGrade(Player p, String job) {
        return playerGrades
                .getOrDefault(p.getUniqueId(), new HashMap<>())
                .getOrDefault(job, "D");
    }

    // 승급 조건 체크 & 승급
    public void tryPromote(Player p, String job, int activityCount, int jobLevel, int questClearCount, int sales) {
        YamlConfiguration yaml = plugin.getConfig("grades");
        for (String grade : yaml.getConfigurationSection("grades").getKeys(false)) {
            // D~S 등급, 조건 판별
            String req = yaml.getString("grades." + grade + ".requirement");
            boolean canUpgrade = false;
            switch (grade) {
                case "C": canUpgrade = activityCount >= 200; break;
                case "B": canUpgrade = activityCount >= 500 || questClearCount >= 10; break;
                case "A": canUpgrade = jobLevel >= 20; break;
                case "S": canUpgrade = questClearCount >= 30 || sales >= 10000; break;
                default: canUpgrade = true;
            }
            if (canUpgrade) upgradeGrade(p, job, grade);
        }
    }

    // 등급 승급 적용
    public void upgradeGrade(Player p, String job, String grade) {
        playerGrades.computeIfAbsent(p.getUniqueId(), k->new HashMap<>()).put(job, grade);
        applyGradeReward(p, job, grade);
        p.sendMessage("§d" + job + " 직업 등급이 [" + grade + "]로 승급되었습니다!");
    }

    // 등급별 보상 적용
    public void applyGradeReward(Player p, String job, String grade) {
        YamlConfiguration yaml = plugin.getConfig("grades");
        String gradeName = yaml.getString("grades." + grade + ".name");
        String reward = yaml.getString("grades." + grade + ".reward");
        p.sendMessage("§6등급 보상 지급: " + reward);
        // 예시: 상점 할인/포인트/아이템/칭호 지급 등
        // 실제 구현은 EconomyManager, TraitManager 등과 연동
    }

    public void saveAll() {
        // TODO: playerGrades 저장
    }

    private void loadGrades() {
        // TODO: playerGrades 파일/DB 로딩
    }
}