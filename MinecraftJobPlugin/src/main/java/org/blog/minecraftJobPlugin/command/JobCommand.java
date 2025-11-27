package org.blog.minecraftJobPlugin.command;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.job.JobMeta;
import org.blog.minecraftJobPlugin.gui.JobSelectionGUI;
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

        if (args.length == 0) {
            new JobSelectionGUI(plugin, player).open();
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            String active = jobManager.getActiveJob(player);
            player.sendMessage("§a현재 장착 직업: " + (active != null ? active : "없음"));
            player.sendMessage("§e보유 직업: " + String.join(", ", jobManager.getPlayerJobs(player)));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§e전체 직업 목록:");
            for (JobMeta meta : jobManager.getAllJobs()) {
                player.sendMessage("§6" + meta.display + "§f - " + meta.description);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("roll")) {
            String job = jobManager.rollRandomJob(player);
            if (job != null) {
                player.sendMessage("§a새로운 직업을 획득했습니다: §b" + job);
            } else {
                player.sendMessage("§c모든 직업을 이미 획득하였습니다.");
            }
            return true;
        }

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

        player.sendMessage("§e사용법: /job, /job info, /job list, /job roll, /job select <직업>");
        return true;
    }
}