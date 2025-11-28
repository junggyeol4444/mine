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
        economyManager.addMoney(player, meta.reward);
        Map<String, QuestProgress> m = progresses.get(player.getUniqueId());
        if (m != null) m.remove(questId);
        savePlayerProgressAsync(player.getUniqueId());
        player.sendMessage("§6퀘스트 완료! 보상: " + meta.reward + plugin.getConfig().getString("economy.currency_symbol","원"));
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