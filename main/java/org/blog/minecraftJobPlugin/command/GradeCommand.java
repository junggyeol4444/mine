package org.blog.minecraftJobPlugin.command;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobGradeManager;
import org.blog.minecraftJobPlugin.manager.JobManager;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.List;  // 추가
import java.util.Set;

/**
 * /grade 명령어
 * - /grade - 현재 등급 보기
 * - /grade list - 모든 직업 등급 보기
 * - /grade info <등급> - 등급 상세 정보
 * - /grade check - 승급 가능 여부 확인
 */
public class GradeCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final JobGradeManager gradeManager;
    private final PluginDataUtil dataUtil;

    public GradeCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.gradeManager = plugin.getGradeManager();
        this.dataUtil = new PluginDataUtil(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c게임 내에서만 사용 가능합니다.");
            return true;
        }

        Player player = (Player) sender;

        // /grade - 현재 등급
        if (args.length == 0) {
            String activeJob = jobManager.getActiveJob(player);
            if (activeJob == null) {
                player.sendMessage("§c직업을 먼저 장착해주세요.");
                return true;
            }

            String grade = gradeManager.getGrade(player, activeJob);
            int activity = gradeManager.getActivity(player, activeJob);
            int questCount = gradeManager.getQuestCount(player, activeJob);

            YamlConfiguration grades = dataUtil.loadGlobal("grades");
            String gradeName = grades.getString("grades." + grade + ".name", grade);
            String nextGrade = getNextGrade(grade);

            player.sendMessage("§a━━━━━━ " + activeJob + " 등급 정보 ━━━━━━");
            player.sendMessage("§6현재 등급: §e" + grade + " §7(" + gradeName + ")");
            player.sendMessage("§6활동 횟수: §f" + activity);
            player.sendMessage("§6완료 퀘스트: §f" + questCount);

            if (nextGrade != null) {
                String requirement = grades.getString("grades." + nextGrade + ".requirement", "");
                player.sendMessage("§6다음 등급: §e" + nextGrade);
                player.sendMessage("§7조건: §f" + requirement);
            } else {
                player.sendMessage("§a최고 등급 달성!");
            }
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        // /grade list - 모든 직업 등급
        if (args[0].equalsIgnoreCase("list")) {
            List<String> jobs = jobManager.getPlayerJobs(player);

            if (jobs.isEmpty()) {
                player.sendMessage("§c보유한 직업이 없습니다.");
                return true;
            }

            player.sendMessage("§a━━━━━━ 내 직업 등급 ━━━━━━");
            YamlConfiguration grades = dataUtil.loadGlobal("grades");

            for (String job : jobs) {
                String grade = gradeManager.getGrade(player, job);
                String gradeName = grades.getString("grades." + grade + ".name", grade);
                int activity = gradeManager.getActivity(player, job);

                player.sendMessage("§6" + job + " §f- §e" + grade + " §7(" + gradeName + ") " +
                        "§f활동: " + activity);
            }
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        // /grade info <등급> - 등급 정보
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                player.sendMessage("§c사용법: /grade info <등급>");
                player.sendMessage("§e등급: D, C, B, A, S");
                return true;
            }

            String grade = args[1].toUpperCase();
            YamlConfiguration grades = dataUtil.loadGlobal("grades");

            if (!grades.isConfigurationSection("grades." + grade)) {
                player.sendMessage("§c존재하지 않는 등급입니다.");
                return true;
            }

            String name = grades.getString("grades." + grade + ".name", grade);
            String requirement = grades.getString("grades." + grade + ".requirement", "없음");
            String reward = grades.getString("grades." + grade + ".reward", "없음");

            player.sendMessage("§a━━━━━━ 등급 정보: " + grade + " ━━━━━━");
            player.sendMessage("§6등급명: §e" + name);
            player.sendMessage("§6승급 조건: §f" + requirement);
            player.sendMessage("§6보상: §f" + reward);
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        // /grade check - 승급 체크
        if (args[0].equalsIgnoreCase("check")) {
            String activeJob = jobManager.getActiveJob(player);
            if (activeJob == null) {
                player.sendMessage("§c직업을 먼저 장착해주세요.");
                return true;
            }

            gradeManager.checkAndUpgrade(player, activeJob);
            return true;
        }

        player.sendMessage("§e사용법:");
        player.sendMessage("§f/grade §7- 현재 등급 보기");
        player.sendMessage("§f/grade list §7- 모든 직업 등급");
        player.sendMessage("§f/grade info <등급> §7- 등급 정보");
        player.sendMessage("§f/grade check §7- 승급 확인");
        return true;
    }

    private String getNextGrade(String current) {
        switch (current) {
            case "D": return "C";
            case "C": return "B";
            case "B": return "A";
            case "A": return "S";
            default: return null;
        }
    }
}