package org.blog.minecraftJobPlugin.listener;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 플레이어 접속/퇴장 시 로드/저장 처리 (간단)
 */
public class JobEventListener implements Listener {

    private final JobPlugin plugin;

    public JobEventListener(JobPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // 퇴장시 비동기 저장
        plugin.getJobManager().saveAllAsync();
        plugin.getEconomyManager().saveAllAsync();
        plugin.getQuestManager().saveAllAsync();
        plugin.getSkillManager().saveAllAsync();
        plugin.getTraitManager().saveAllAsync();
        plugin.getComboManager().saveAllAsync();
        plugin.getGradeManager().saveAllAsync();
    }
}