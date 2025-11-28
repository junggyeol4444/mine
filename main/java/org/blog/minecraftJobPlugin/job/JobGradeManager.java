package org.blog.minecraftJobPlugin.job;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.manager.JobManager;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * JobGradeManager - 직업 등급 시스템
 * - 활동 추적
 * - 자동 등급 상승
 * - 등급별 보상
 */
public class JobGradeManager {
    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final PluginDataUtil dataUtil;

    // 등급 저장
    private final Map<UUID, Map<String, String>> playerGrades = new HashMap<>();
    // 활동 횟수 저장
    private final Map<UUID, Map<String, Integer>> activityCount = new HashMap<>();
    // 퀘스트 완료 횟수 저장
    private final Map<UUID, Map<String, Integer>> questCount = new HashMap<>();

    public JobGradeManager(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.dataUtil = new PluginDataUtil(plugin);
        loadGrades();
    }

    /**
     * 플레이어의 특정 직업 등급 조회
     */
    public String getGrade(Player p, String job) {
        return playerGrades.getOrDefault(p.getUniqueId(), Collections.emptyMap()).getOrDefault(job, "D");
    }

    /**
     * 활동 횟수 조회
     */
    public int getActivity(Player p, String job) {
        return activityCount.getOrDefault(p.getUniqueId(), Collections.emptyMap()).getOrDefault(job, 0);
    }

    /**
     * 퀘스트 완료 횟수 조회
     */
    public int getQuestCount(Player p, String job) {
        return questCount.getOrDefault(p.getUniqueId(), Collections.emptyMap()).getOrDefault(job, 0);
    }

    /**
     * 활동 횟수 증가 (블록 채굴, 몹 처치 등)
     */
    public void addActivity(Player p, String job, int amount) {
        UUID id = p.getUniqueId();
        Map<String, Integer> map = activityCount.computeIfAbsent(id, k -> new HashMap<>());
        map.put(job, map.getOrDefault(job, 0) + amount);

        // 자동 승급 체크
        checkAndUpgrade(p, job);
        savePlayerGradesAsync(id);
    }

    /**
     * 퀘스트 완료 횟수 증가
     */
    public void addQuestCompletion(Player p, String job) {
        UUID id = p.getUniqueId();
        Map<String, Integer> map = questCount.computeIfAbsent(id, k -> new HashMap<>());
        map.put(job, map.getOrDefault(job, 0) + 1);

        // 자동 승급 체크
        checkAndUpgrade(p, job);
        savePlayerGradesAsync(id);
    }

    /**
     * 승급 가능 여부 확인 및 자동 승급
     */
    public void checkAndUpgrade(Player p, String job) {
        String currentGrade = getGrade(p, job);
        String nextGrade = getNextGrade(currentGrade);

        if (nextGrade == null) {
            return; // 이미 최고 등급
        }

        if (canUpgrade(p, job, nextGrade)) {
            upgradeGrade(p, job, nextGrade);
        }
    }

    /**
     * 승급 가능 여부 체크
     */
    private boolean canUpgrade(Player p, String job, String targetGrade) {
        int activity = getActivity(p, job);
        int quests = getQuestCount(p, job);

        switch (targetGrade) {
            case "C":
                return activity >= 200; // 활동 200회

            case "B":
                return activity >= 500 || quests >= 10; // 활동 500회 또는 퀘스트 10회

            case "A":
                return activity >= 1000 && quests >= 20; // 활동 1000회 AND 퀘스트 20회

            case "S":
                // 특별 조건: 모든 스킬 레벨 5 이상, 활동 2000회, 퀘스트 50회
                return activity >= 2000 && quests >= 50 && checkSkillLevels(p, job);

            default:
                return false;
        }
    }

    /**
     * 스킬 레벨 체크 (S등급용)
     */
    private boolean checkSkillLevels(Player p, String job) {
        if (plugin.getSkillManager() == null) {
            return true;
        }

        JobMeta meta = jobManager.getJobMeta(job);
        if (meta == null || meta.skills.isEmpty()) {
            return true;
        }

        for (String skill : meta.skills) {
            int level = plugin.getSkillManager().getSkillLevel(p, skill);
            if (level < 5) {
                return false;
            }
        }
        return true;
    }

    /**
     * 등급 상승
     */
    public void upgradeGrade(Player p, String job, String grade) {
        playerGrades.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(job, grade);
        savePlayerGradesAsync(p.getUniqueId());

        YamlConfiguration grades = dataUtil.loadGlobal("grades");
        String gradeName = grades.getString("grades." + grade + ".name", grade);
        String reward = grades.getString("grades." + grade + ".reward", "");

        p.sendMessage("§a━━━━━━ 등급 상승! ━━━━━━");
        p.sendMessage("§6" + job + " §f등급이 §e" + grade + " §f(§e" + gradeName + "§f)로 승급되었습니다!");
        p.sendMessage("§6보상: §f" + reward);
        p.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");

        // 보상 적용
        applyGradeReward(p, job, grade);
    }

    /**
     * 등급 보상 적용
     */
    private void applyGradeReward(Player p, String job, String grade) {
        switch (grade) {
            case "C":
                // 수습: 스킬 경험치 보너스
                p.sendMessage("§d[등급 보상] 스킬 경험치 획득량 +10%");
                break;

            case "B":
                // 숙련: 특성 포인트 + 상점 할인
                if (plugin.getTraitManager() != null) {
                    plugin.getTraitManager().addPoint(p, job, 2);
                }
                p.sendMessage("§d[등급 보상] 특성 포인트 +2, 상점 할인 5%");
                break;

            case "A":
                // 전문가: 희귀 스킬 해금 + 상점 할인
                if (plugin.getTraitManager() != null) {
                    plugin.getTraitManager().addPoint(p, job, 3);
                }
                p.sendMessage("§d[등급 보상] 특성 포인트 +3, 상점 할인 10%");
                break;

            case "S":
                // 마스터: 최상위 보상
                if (plugin.getTraitManager() != null) {
                    plugin.getTraitManager().addPoint(p, job, 5);
                }
                if (plugin.getEconomyManager() != null) {
                    plugin.getEconomyManager().addMoney(p, 10000);
                }
                p.sendMessage("§d[등급 보상] 특성 포인트 +5, 상점 할인 20%, 10000원");
                p.sendMessage("§6§l칭호 획득: [" + job + " 마스터]");
                break;
        }
    }

    /**
     * 등급별 상점 할인율
     */
    public double getShopDiscount(Player p, String job) {
        String grade = getGrade(p, job);
        switch (grade) {
            case "B": return 0.05; // 5%
            case "A": return 0.10; // 10%
            case "S": return 0.20; // 20%
            default: return 0.0;
        }
    }

    /**
     * 등급별 스킬 경험치 보너스
     */
    public double getSkillExpBonus(Player p, String job) {
        String grade = getGrade(p, job);
        switch (grade) {
            case "C": return 0.10; // +10%
            case "B": return 0.20; // +20%
            case "A": return 0.30; // +30%
            case "S": return 0.50; // +50%
            default: return 0.0;
        }
    }

    private String getNextGrade(String current) {
        switch (current) {
            case "D": return "C";
            case "C": return "B";
            case "B": return "A";
            case "A": return "S";
            default: return null;
        }
    }

    private void loadGrades() {
        java.io.File playerDir = new java.io.File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) {
            return;
        }

        java.io.File[] files = playerDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (java.io.File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

                Map<String, String> grades = new HashMap<>();
                Map<String, Integer> activity = new HashMap<>();
                Map<String, Integer> quests = new HashMap<>();

                if (cfg.isConfigurationSection("grades")) {
                    for (String job : cfg.getConfigurationSection("grades").getKeys(false)) {
                        grades.put(job, cfg.getString("grades." + job + ".grade", "D"));
                        activity.put(job, cfg.getInt("grades." + job + ".activity", 0));
                        quests.put(job, cfg.getInt("grades." + job + ".quests", 0));
                    }
                }

                if (!grades.isEmpty()) playerGrades.put(uuid, grades);
                if (!activity.isEmpty()) activityCount.put(uuid, activity);
                if (!quests.isEmpty()) questCount.put(uuid, quests);
            } catch (Exception ignored) {
            }
        }
    }

    private void savePlayerGradesAsync(UUID uuid) {
        YamlConfiguration cfg = dataUtil.loadPlayerConfig(uuid);
        Map<String, String> grades = playerGrades.getOrDefault(uuid, Collections.emptyMap());
        Map<String, Integer> activity = activityCount.getOrDefault(uuid, Collections.emptyMap());
        Map<String, Integer> quests = questCount.getOrDefault(uuid, Collections.emptyMap());

        for (String job : grades.keySet()) {
            cfg.set("grades." + job + ".grade", grades.get(job));
            cfg.set("grades." + job + ".activity", activity.getOrDefault(job, 0));
            cfg.set("grades." + job + ".quests", quests.getOrDefault(job, 0));
        }
        dataUtil.savePlayerConfigAsync(uuid, cfg);
    }

    public void saveAllAsync() {
        for (UUID u : playerGrades.keySet()) {
            savePlayerGradesAsync(u);
        }
    }

    public void saveAll() {
        for (UUID u : playerGrades.keySet()) {
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(u);
            Map<String, String> grades = playerGrades.getOrDefault(u, Collections.emptyMap());
            Map<String, Integer> activity = activityCount.getOrDefault(u, Collections.emptyMap());
            Map<String, Integer> quests = questCount.getOrDefault(u, Collections.emptyMap());

            for (String job : grades.keySet()) {
                cfg.set("grades." + job + ".grade", grades.get(job));
                cfg.set("grades." + job + ".activity", activity.getOrDefault(job, 0));
                cfg.set("grades." + job + ".quests", quests.getOrDefault(job, 0));
            }
            dataUtil.savePlayerConfigSync(u, cfg);
        }
    }
}