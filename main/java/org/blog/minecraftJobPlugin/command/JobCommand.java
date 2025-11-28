package org.blog.minecraftJobPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.Job;
import org.blog.minecraftJobPlugin.listeners.JobGuiListener;
import org.blog.minecraftJobPlugin.manager.JobManager;

import java.util.List;

public class JobCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final JobManager jobManager;

    public JobCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 플레이어만 사용 가능
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        // 인자 없이 /job 입력 시 GUI 열기
        if (args.length == 0) {
            openJobGui(player);
            return true;
        }

        // 하위 명령어 처리
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                showJobList(player);
                break;

            case "info":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /job info <직업이름>");
                    return true;
                }
                showJobInfo(player, args[1]);
                break;

            case "my":
            case "myinfo":
                showMyJobs(player);
                break;

            case "shop":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /job shop <직업이름>");
                    return true;
                }
                openShop(player, args[1]);
                break;

            case "skills":
            case "skilltree":
                openSkillTree(player);
                break;

            case "grade":
                showGrade(player);
                break;

            case "reload":
                if (!player.hasPermission("jobplugin.admin")) {
                    player.sendMessage("§c권한이 없습니다.");
                    return true;
                }
                reloadPlugin(player);
                break;

            case "debug":
                if (!player.hasPermission("jobplugin.admin")) {
                    player.sendMessage("§c권한이 없습니다.");
                    return true;
                }
                jobManager.printDebugInfo();
                player.sendMessage("§a디버그 정보를 콘솔에 출력했습니다.");
                break;

            default:
                player.sendMessage("§c알 수 없는 명령어입니다. /job 을 입력하여 GUI를 여세요.");
                break;
        }

        return true;
    }

    /**
     * 직업 선택 GUI 열기
     */
    private void openJobGui(Player player) {
        // JobGuiListener를 통해 GUI 열기
        JobGuiListener listener = new JobGuiListener(plugin);
        listener.openJobSelectionGui(player);
    }

    /**
     * 모든 직업 목록 표시
     */
    private void showJobList(Player player) {
        List<Job> allJobs = jobManager.getAllJobs();

        if (allJobs.isEmpty()) {
            player.sendMessage("§c사용 가능한 직업이 없습니다.");
            return;
        }

        player.sendMessage("§6§l========== 직업 목록 ==========");

        for (Job job : allJobs) {
            boolean owned = jobManager.hasJob(player, job.getName());
            String status = owned ? "§a[보유]" : "§c[미보유]";

            player.sendMessage(status + " §f" + job.getName() + " §7- " + job.getDescription());
        }

        player.sendMessage("§6§l==============================");
        player.sendMessage("§7자세한 정보: §f/job info <직업이름>");
    }

    /**
     * 특정 직업 정보 표시
     */
    private void showJobInfo(Player player, String jobName) {
        Job job = jobManager.getJob(jobName);

        if (job == null) {
            player.sendMessage("§c존재하지 않는 직업입니다: " + jobName);
            return;
        }

        boolean owned = jobManager.hasJob(player, jobName);
        String activeJob = jobManager.getActiveJob(player);
        boolean active = jobName.equals(activeJob);

        player.sendMessage("§6§l========== " + job.getName() + " ==========");
        player.sendMessage("§f설명: §7" + job.getDescription());
        player.sendMessage("§f상태: " + (owned ? "§a보유" : "§c미보유") +
                (active ? " §e(장착중)" : ""));

        if (!owned) {
            double cost = calculateJobCost(player, jobName);
            String currency = plugin.getConfig().getString("economy.currency_symbol", "원");
            if (cost > 0) {
                player.sendMessage("§f획득 비용: §e" + (int)cost + currency);
            } else {
                player.sendMessage("§f획득 비용: §a무료");
            }
        }

        player.sendMessage("§6§l===========================");
    }

    /**
     * 내 직업 정보 표시
     */
    private void showMyJobs(Player player) {
        List<String> myJobs = jobManager.getPlayerJobs(player);
        String activeJob = jobManager.getActiveJob(player);

        if (myJobs.isEmpty()) {
            player.sendMessage("§c보유한 직업이 없습니다. §7/job §f으로 직업을 선택하세요.");
            return;
        }

        player.sendMessage("§6§l========== 내 직업 ==========");
        player.sendMessage("§f보유 직업: §b" + myJobs.size() + "개");

        for (String jobName : myJobs) {
            boolean active = jobName.equals(activeJob);
            String status = active ? "§a[장착중]" : "§7[보유]";
            player.sendMessage(status + " §f" + jobName);
        }

        player.sendMessage("§6§l===========================");

        if (activeJob == null) {
            player.sendMessage("§7장착된 직업이 없습니다. §f/job §7으로 직업을 장착하세요.");
        }
    }

    /**
     * 상점 열기
     */
    private void openShop(Player player, String jobName) {
        Job job = jobManager.getJob(jobName);

        if (job == null) {
            player.sendMessage("§c존재하지 않는 직업입니다: " + jobName);
            return;
        }

        // TODO: ShopGUI 구현
        player.sendMessage("§e" + jobName + " 상점은 아직 구현 중입니다.");
    }

    /**
     * 스킬 트리 열기
     */
    private void openSkillTree(Player player) {
        // TODO: SkillTreeGUI 구현
        player.sendMessage("§e스킬 트리는 아직 구현 중입니다.");
    }

    /**
     * 등급 정보 표시
     */
    private void showGrade(Player player) {
        String activeJob = jobManager.getActiveJob(player);

        if (activeJob == null) {
            player.sendMessage("§c장착된 직업이 없습니다.");
            return;
        }

        // TODO: GradeManager를 통한 등급 정보 표시
        player.sendMessage("§e등급 시스템은 아직 구현 중입니다.");
    }

    /**
     * 플러그인 리로드
     */
    private void reloadPlugin(Player player) {
        player.sendMessage("§e플러그인을 리로드하는 중...");

        try {
            plugin.reloadConfig();
            jobManager.reload();

            player.sendMessage("§a플러그인 리로드 완료!");
        } catch (Exception e) {
            player.sendMessage("§c리로드 중 오류 발생: " + e.getMessage());
            plugin.getLogger().severe("리로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 직업 획득 비용 계산 (JobGuiListener와 동일한 로직)
     */
    private double calculateJobCost(Player player, String jobName) {
        if (plugin.getConfig().contains("job.job_specific_costs." + jobName)) {
            return plugin.getConfig().getDouble("job.job_specific_costs." + jobName);
        }

        int ownedJobCount = jobManager.getPlayerJobs(player).size();

        switch (ownedJobCount) {
            case 0:
                return plugin.getConfig().getDouble("job.first_job_cost", 0);
            case 1:
                return plugin.getConfig().getDouble("job.second_job_cost", 50000);
            case 2:
                return plugin.getConfig().getDouble("job.third_job_cost", 100000);
            default:
                return plugin.getConfig().getDouble("job.additional_job_cost", 200000);
        }
    }
}