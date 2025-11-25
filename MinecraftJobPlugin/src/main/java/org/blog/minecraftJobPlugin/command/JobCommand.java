package com.yourname.jobplugin.command;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.job.JobManager;
import com.yourname.jobplugin.job.JobMeta;
import com.yourname.jobplugin.gui.JobSelectionGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class JobCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final JobManager jobManager;

    public JobCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("[직업] 게임 내에서만 사용할 수 있습니다.");
            return true;
        }
        Player player = (Player)sender;

        // /job (GUI 열기)
        if (args.length == 0) {
            JobSelectionGUI gui = new JobSelectionGUI(plugin, player);
            gui.open();
            return true;
        }
        // /job info
        if (args[0].equalsIgnoreCase("info")) {
            String active = jobManager.getActiveJob(player);
            player.sendMessage("§a현재 장착 직업: " + (active != null ? active : "없음"));
            player.sendMessage("§e보유 직업: " + String.join(", ", jobManager.getPlayerJobs(player)));
            return true;
        }
        // /job list
        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§e전체 직업 목록:");
            for (JobMeta meta : jobManager.getAllJobs()) {
                player.sendMessage("§6" + meta.display + "§f - " + meta.description);
            }
            return true;
        }
        // /job roll
        if (args[0].equalsIgnoreCase("roll")) {
            // 돈 체크/소모 (이코노미 연동 가능)
            String job = jobManager.rollRandomJob(player);
            if (job != null) {
                player.sendMessage("§a새로운 직업을 획득했습니다: §b" + job);
            } else {
                player.sendMessage("§c모든 직업을 이미 획득하였습니다.");
            }
            return true;
        }
        // /job select <직업명>
        if (args[0].equalsIgnoreCase("select") && args.length > 1) {
            String targetJob = args[1];
            if (!jobManager.hasJob(player, targetJob)) {
                player.sendMessage("§c해당 직업을 보유하지 않았습니다.");
            } else {
                jobManager.setActiveJob(player, targetJob);
                player.sendMessage("§a장착 직업 변경: " + targetJob);
            }
            return true;
        }

        player.sendMessage("§e/job /job info /job list /job roll /job select <직업>");
        return true;
    }
}