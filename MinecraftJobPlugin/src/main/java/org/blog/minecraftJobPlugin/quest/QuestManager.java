package com.yourname.jobplugin.quest;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.job.JobManager;
import com.yourname.jobplugin.econ.EconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

public class QuestManager {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final EconomyManager economyManager;

    // [플레이어 UUID] → 진행중 퀘스트, 완료 등
    private final Map<UUID, Map<String, QuestProgress>> progresses = new HashMap<>();

    public QuestManager(JobPlugin plugin, JobManager jobManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.economyManager = economyManager;
        // TODO: 진행도 로드
    }

    public List<QuestMeta> getJobQuests(String job) {
        YamlConfiguration questsYaml = plugin.getConfig("quests");
        List<QuestMeta> result = new ArrayList<>();
        List<?> questList = questsYaml.getList("quests." + job, new ArrayList<>());
        for (Object obj : questList) {
            if (obj instanceof Map) result.add(QuestMeta.fromYaml((Map<?,?>)obj));
        }
        return result;
    }

    // 퀘스트 진행/완료/보상 지급
    public void submitQuest(Player player, String questId) {
        // 완료 처리, 보상 지급
        QuestMeta meta = findQuestById(player, questId);
        if (meta == null) return;
        economyManager.addMoney(player, meta.reward);
        player.sendMessage("§6퀘스트 클리어! 보상으로 " + meta.reward + "원 지급.");
        // TODO: 경험치, 등급, 특성포인트 지급 등 확장
    }

    private QuestMeta findQuestById(Player player, String questId) {
        String job = jobManager.getActiveJob(player);
        for (QuestMeta q : getJobQuests(job)) if (q.id.equals(questId)) return q;
        return null;
    }

    // 퀘스트 진행도 관리
    public void updateProgress(Player player, String questId, int progress) {
        progresses.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        progresses.get(player.getUniqueId()).put(questId, new QuestProgress(questId, progress));
        // TODO: 클리어 시 submitQuest 호출
    }

    public void saveAll() {
        // TODO: 플레이어별 퀘스트진행도 저장 구현
    }
}