package org.blog.minecraftJobPlugin.listener;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobComboManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 플레이어가 접속할 때 자동으로 조합 체크
 */
public class ComboCheckListener implements Listener {

    private final JobPlugin plugin;
    private final JobComboManager comboManager;

    public ComboCheckListener(JobPlugin plugin) {
        this.plugin = plugin;
        this.comboManager = plugin.getComboManager();
    }

    /**
     * 플레이어 접속 시 조합 자동 체크
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        
        // 1초 후 조합 체크 (데이터 로딩 대기)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            comboManager.checkAndUnlockCombos(player);
        }, 20L);
    }
}