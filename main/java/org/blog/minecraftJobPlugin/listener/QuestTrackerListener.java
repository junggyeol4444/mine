package org.blog.minecraftJobPlugin.listener;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.manager.JobManager;  // job → manager로 수정
import org.blog.minecraftJobPlugin.quest.QuestManager;
import org.blog.minecraftJobPlugin.quest.QuestMeta;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

import java.util.List;

/**
 * 퀘스트 진행도를 자동으로 추적하는 리스너
 * 
 * 지원하는 퀘스트 타입:
 * - collect_block: 블록 채집 (예: 철광석 30개)
 * - kill_mob: 몹 처치 (예: 몹 20마리)
 * - harvest: 작물 수확 (예: 밀 200개)
 * - catch_rare: 희귀 물고기 낚시 (예: 5마리)
 */
public class QuestTrackerListener implements Listener {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final QuestManager questManager;

    public QuestTrackerListener(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.questManager = plugin.getQuestManager();
    }

    /**
     * 블록 채집 퀘스트 (collect_block)
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        String job = jobManager.getActiveJob(player);
        if (job == null) return;

        Material broken = e.getBlock().getType();
        
        // 활동 횟수 증가 (등급 시스템)
        plugin.getGradeManager().addActivity(player, job, 1);
        
        List<QuestMeta> quests = questManager.getJobQuests(job);

        for (QuestMeta quest : quests) {
            if (!"collect_block".equals(quest.type)) continue;
            
            // quests.yml에서 objective.block 확인
            String targetBlock = questManager.getQuestObjective(quest.id, "block");
            if (targetBlock != null && targetBlock.equalsIgnoreCase(broken.name())) {
                questManager.updateProgress(player, quest.id, 1);
                
                int current = questManager.getProgress(player, quest.id);
                int target = questManager.getQuestObjective(quest.id, "amount", 1);
                
                if (current >= target) {
                    player.sendMessage("§a[퀘스트] " + quest.name + " §6완료! §e/quest submit " + quest.id + " §6로 보상을 받으세요!");
                } else {
                    player.sendMessage("§7[퀘스트] " + quest.name + " §f(" + current + "/" + target + ")");
                }
            }
        }
    }

    /**
     * 몹 처치 퀘스트 (kill_mob)
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity().getKiller() == null) return;
        Player player = e.getEntity().getKiller();
        String job = jobManager.getActiveJob(player);
        if (job == null) return;

        // 활동 횟수 증가 (등급 시스템)
        plugin.getGradeManager().addActivity(player, job, 1);
        
        List<QuestMeta> quests = questManager.getJobQuests(job);

        for (QuestMeta quest : quests) {
            if (!"kill_mob".equals(quest.type)) continue;
            
            // 특정 몹 타입 지정이 있으면 체크, 없으면 모든 몹
            String targetMob = questManager.getQuestObjective(quest.id, "mob");
            if (targetMob != null && !targetMob.isEmpty()) {
                if (!e.getEntity().getType().name().equalsIgnoreCase(targetMob)) {
                    continue;
                }
            }
            
            questManager.updateProgress(player, quest.id, 1);
            
            int current = questManager.getProgress(player, quest.id);
            int target = questManager.getQuestObjective(quest.id, "amount", 20);
            
            if (current >= target) {
                player.sendMessage("§a[퀘스트] " + quest.name + " §6완료! §e/quest submit " + quest.id + " §6로 보상을 받으세요!");
            } else if (current % 5 == 0) { // 5마리마다 알림
                player.sendMessage("§7[퀘스트] " + quest.name + " §f(" + current + "/" + target + ")");
            }
        }
    }

    /**
     * 작물 수확 퀘스트 (harvest)
     */
    @EventHandler
    public void onHarvest(PlayerHarvestBlockEvent e) {
        Player player = e.getPlayer();
        String job = jobManager.getActiveJob(player);
        if (job == null) return;

        Material harvested = e.getHarvestedBlock().getType();
        int amount = e.getItemsHarvested().stream().mapToInt(item -> item.getAmount()).sum();
        
        // 활동 횟수 증가 (등급 시스템)
        plugin.getGradeManager().addActivity(player, job, amount);
        
        List<QuestMeta> quests = questManager.getJobQuests(job);

        for (QuestMeta quest : quests) {
            if (!"harvest".equals(quest.type)) continue;
            
            String targetCrop = questManager.getQuestObjective(quest.id, "crop");
            if (targetCrop != null && targetCrop.equalsIgnoreCase(harvested.name())) {
                questManager.updateProgress(player, quest.id, amount);
                
                int current = questManager.getProgress(player, quest.id);
                int target = questManager.getQuestObjective(quest.id, "amount", 200);
                
                if (current >= target) {
                    player.sendMessage("§a[퀘스트] " + quest.name + " §6완료! §e/quest submit " + quest.id + " §6로 보상을 받으세요!");
                } else if (current % 50 == 0) { // 50개마다 알림
                    player.sendMessage("§7[퀘스트] " + quest.name + " §f(" + current + "/" + target + ")");
                }
            }
        }
    }

    /**
     * 낚시 퀘스트 (catch_rare)
     */
    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = e.getPlayer();
        String job = jobManager.getActiveJob(player);
        if (job == null) return;

        // 활동 횟수 증가 (등급 시스템)
        plugin.getGradeManager().addActivity(player, job, 1);
        
        List<QuestMeta> quests = questManager.getJobQuests(job);

        for (QuestMeta quest : quests) {
            if (!"catch_rare".equals(quest.type)) continue;
            
            // 희귀 판정 (보물 또는 특정 물고기)
            if (e.getCaught() != null) {
                boolean isRare = false;
                
                // 보물 카테고리면 희귀
                if (e.getCaught() instanceof org.bukkit.entity.Item) {
                    org.bukkit.entity.Item item = (org.bukkit.entity.Item) e.getCaught();
                    Material type = item.getItemStack().getType();
                    
                    // 희귀 물고기: 연어, 열대어, 복어
                    if (type == Material.SALMON || type == Material.TROPICAL_FISH || 
                        type == Material.PUFFERFISH || type == Material.ENCHANTED_BOOK ||
                        type == Material.NAME_TAG || type == Material.SADDLE) {
                        isRare = true;
                    }
                }
                
                if (isRare) {
                    questManager.updateProgress(player, quest.id, 1);
                    
                    int current = questManager.getProgress(player, quest.id);
                    int target = questManager.getQuestObjective(quest.id, "amount", 5);
                    
                    if (current >= target) {
                        player.sendMessage("§a[퀘스트] " + quest.name + " §6완료! §e/quest submit " + quest.id + " §6로 보상을 받으세요!");
                    } else {
                        player.sendMessage("§7[퀘스트] " + quest.name + " §f(" + current + "/" + target + ")");
                    }
                }
            }
        }
    }
}