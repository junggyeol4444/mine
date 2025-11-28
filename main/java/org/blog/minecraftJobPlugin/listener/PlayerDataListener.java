package org.blog.minecraftJobPlugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.blog.minecraftJobPlugin.JobPlugin;

public class PlayerDataListener implements Listener {

    private final JobPlugin plugin;

    public PlayerDataListener(JobPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 플레이어 접속 시 데이터 로드
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 모든 매니저의 데이터 로드
        plugin.getJobManager().loadPlayerData(player);
        plugin.getSkillManager().loadPlayerData(player);
        plugin.getQuestManager().loadPlayerData(player);
        plugin.getEconomyManager().loadPlayerData(player);
        plugin.getEquipmentManager().loadPlayerData(player);
        plugin.getGradeManager().loadPlayerData(player);
        plugin.getTraitManager().loadPlayerData(player);

        // 직업 조합 체크
        if (plugin.getConfig().getBoolean("combo.auto_check_on_join", true)) {
            plugin.getComboManager().checkCombos(player);
        }
    }

    /**
     * 플레이어 퇴장 시 데이터 저장 (비동기)
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("performance.async_save", true)) {
            // 비동기 저장
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                savePlayerData(player);
            });
        } else {
            // 동기 저장
            savePlayerData(player);
        }
    }

    /**
     * 플레이어 데이터 저장
     */
    private void savePlayerData(Player player) {
        try {
            plugin.getJobManager().savePlayerData(player);
            plugin.getSkillManager().savePlayerData(player);
            plugin.getQuestManager().savePlayerData(player);
            plugin.getEconomyManager().savePlayerData(player);
            plugin.getEquipmentManager().savePlayerData(player);
            plugin.getGradeManager().savePlayerData(player);
            plugin.getTraitManager().savePlayerData(player);
        } catch (Exception e) {
            plugin.getLogger().severe("플레이어 데이터 저장 실패 (" + player.getName() + "): " + e.getMessage());
        }
    }
}