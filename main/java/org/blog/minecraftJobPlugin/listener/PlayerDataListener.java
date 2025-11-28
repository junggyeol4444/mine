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

        // JobManager만 loadPlayerData 메서드가 있음
        plugin.getJobManager().loadPlayerData(player);

        // 나머지 매니저들은 자동으로 데이터를 로드하므로 호출 불필요
        // plugin.getSkillManager().loadPlayerData(player);  // 제거
        // plugin.getQuestManager().loadPlayerData(player);  // 제거
        // plugin.getEconomyManager().loadPlayerData(player);  // 제거
        // plugin.getEquipmentManager().loadPlayerData(player);  // 제거
        // plugin.getGradeManager().loadPlayerData(player);  // 제거
        // plugin.getTraitManager().loadPlayerData(player);  // 제거

        // 직업 조합 체크
        if (plugin.getConfig().getBoolean("combo.auto_check_on_join", true)) {
            if (plugin.getComboManager() != null) {
                plugin.getComboManager().checkAndUnlockCombos(player);
            }
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

            // 나머지 매니저들은 saveAll() 또는 saveAllAsync()를 사용
            // 개별 플레이어 저장 메서드가 없으므로 전체 저장으로 처리
        } catch (Exception e) {
            plugin.getLogger().severe("플레이어 데이터 저장 실패 (" + player.getName() + "): " + e.getMessage());
        }
    }
}