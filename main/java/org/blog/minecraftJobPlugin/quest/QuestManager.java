package org.blog.minecraftJobPlugin.quest;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.econ.EconomyManager;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class QuestManager {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final EconomyManager economyManager;
    private final PluginDataUtil dataUtil;

    private final Map<UUID, Map<String, QuestProgress>> progresses = new HashMap<>();

    public QuestManager(JobPlugin plugin, JobManager jobManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.economyManager = economyManager;
        this.dataUtil = new PluginDataUtil(plugin);
        loadAllProgress();
    }

    public List<QuestMeta> getJobQuests(String job) {
        YamlConfiguration yaml = dataUtil.loadGlobal("quests");
        List<QuestMeta> result = new ArrayList<>();
        if (yaml == null) return result;
        List<?> list = yaml.getList("quests." + job, Collections.emptyList());
        for (Object o : list) {
            if (o instanceof Map) result.add(QuestMeta.fromYaml((Map<?,?>)o));
        }
        return result;
    }

    /**
     * 퀘스트 목표 값 조회 (문자열)
     */
    public String getQuestObjective(String questId, String key) {
        YamlConfiguration yaml = dataUtil.loadGlobal("quests");
        if (yaml == null) return null;
        
        // 모든 직업 퀘스트에서 찾기
        for (String job : yaml.getConfigurationSection("quests").getKeys(false)) {
            List<?> quests = yaml.getList("quests." + job, Collections.emptyList());
            for (Object o : quests) {
                if (!(o instanceof Map)) continue;
                Map<?,?> map = (Map<?,?>)o;
                if (questId.equals(String.valueOf(map.get("id")))) {
                    Object objective = map.get("objective");
                    if (objective instanceof Map) {
                        Map<?,?> objMap = (Map<?,?>)objective;
                        Object value = objMap.get(key);
                        return value != null ? String.valueOf(value) : null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 퀘스트 목표 값 조회 (숫자)
     */
    public int getQuestObjective(String questId, String key, int defaultValue) {
        String value = getQuestObjective(questId, key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 현재 진행도 조회
     */
    public int getProgress(Player player, String questId) {
        return progresses
            .getOrDefault(player.getUniqueId(), Collections.emptyMap())
            .getOrDefault(questId, new QuestProgress(questId, 0))
            .progress;
    }

    public void updateProgress(Player player, String questId, int amount) {
        progresses.computeIfAbsent(player.getUniqueId(), k->new HashMap<>());
        QuestProgress p = progresses.get(player.getUniqueId()).get(questId);
        if (p == null) p = new QuestProgress(questId, 0);
        p.progress += amount;
        progresses.get(player.getUniqueId()).put(questId, p);
        savePlayerProgressAsync(player.getUniqueId());
    }

    public void submitQuest(Player player, String questId) {
        QuestMeta meta = findQuestById(player, questId);
        if (meta == null) {
            player.sendMessage("§c퀘스트를 찾을 수 없습니다.");
            return;
        }
        
        // 진행도 확인
        int current = getProgress(player, questId);
        int required = getQuestObjective(questId, "amount", 1);
        
        if (current < required) {
            player.sendMessage("§c퀘스트가 아직 완료되지 않았습니다. (" + current + "/" + required + ")");
            return;
        }
        
        String job = jobManager.getActiveJob(player);
        
        // 보상 지급
        economyManager.addMoney(player, meta.reward);
        
        // 특성 포인트 추가 (퀘스트 완료 시 1포인트)
        if (job != null) {
            plugin.getTraitManager().addPoint(player, job, 1);
            // 퀘스트 완료 횟수 증가 (등급 시스템)
            plugin.getGradeManager().addQuestCompletion(player, job);
        }
        
        // 진행도 초기화
        Map<String, QuestProgress> m = progresses.get(player.getUniqueId());
        if (m != null) m.remove(questId);
        savePlayerProgressAsync(player.getUniqueId());
        
        player.sendMessage("§a━━━━━━ 퀘스트 완료 ━━━━━━");
        player.sendMessage("§6" + meta.name);
        player.sendMessage("§f보상: §e" + meta.reward + plugin.getConfig().getString("economy.currency_symbol","원"));
        player.sendMessage("§f특성 포인트: §d+1");
        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━");
    }

    private QuestMeta findQuestById(Player player, String questId) {
        String job = jobManager.getActiveJob(player);
        if (job == null) return null;
        for (QuestMeta q : getJobQuests(job)) if (q.id.equals(questId)) return q;
        return null;
    }

    private void loadAllProgress() {
        java.io.File playerDir = new java.io.File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) return;
        java.io.File[] files = playerDir.listFiles((d,n)->n.endsWith(".yml"));
        if (files == null) return;
        for (java.io.File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml",""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                Map<String, QuestProgress> map = new HashMap<>();
                if (cfg.isConfigurationSection("quests")) {
                    for (String qid : cfg.getConfigurationSection("quests").getKeys(false)) {
                        int prog = cfg.getInt("quests." + qid + ".progress", 0);
                        map.put(qid, new QuestProgress(qid, prog));
                    }
                }
                if (!map.isEmpty()) progresses.put(uuid, map);
            } catch (Exception ignored) {}
        }
    }

    private void savePlayerProgressAsync(UUID uuid) {
        YamlConfiguration cfg = dataUtil.loadPlayerConfig(uuid);
        Map<String, QuestProgress> map = progresses.getOrDefault(uuid, Collections.emptyMap());
        for (Map.Entry<String, QuestProgress> e : map.entrySet()) {
            cfg.set("quests." + e.getKey() + ".progress", e.getValue().progress);
        }
        dataUtil.savePlayerConfigAsync(uuid, cfg);
    }

    public void saveAllAsync() {
        for (UUID u : progresses.keySet()) savePlayerProgressAsync(u);
    }

    public void saveAll() {
        for (UUID u : progresses.keySet()) {
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(u);
            Map<String, QuestProgress> map = progresses.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String, QuestProgress> e : map.entrySet()) cfg.set("quests." + e.getKey() + ".progress", e.getValue().progress);
            dataUtil.savePlayerConfigSync(u, cfg);
        }
    }
}