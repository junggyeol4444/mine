package org.blog.minecraftJobPlugin.command;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.gui.SkillTreeGUI;
import org.blog.minecraftJobPlugin.job.JobMeta;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.skill.SkillManager;
import org.blog.minecraftJobPlugin.skill.SkillMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SkillCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final SkillManager skillManager;
    private final JobManager jobManager;

    public SkillCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.skillManager = plugin.getSkillManager();
        this.jobManager = plugin.getJobManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("콘솔에서는 사용할 수 없습니다.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            // 기본: 스킬 트리 GUI 열기
            new SkillTreeGUI(plugin, player).open();
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            String job = jobManager.getActiveJob(player);
            if (job == null) {
                player.sendMessage("§c장착된 직업이 없습니다. /job 으로 직업을 선택하세요.");
                return true;
            }
            JobMeta meta = jobManager.getJobMeta(job);
            if (meta == null) {
                player.sendMessage("§c직업 메타를 찾을 수 없습니다.");
                return true;
            }
            player.sendMessage("§e[" + meta.display + "] 스킬 목록:");
            List<String> skills = meta.skills;
            if (skills == null || skills.isEmpty()) {
                player.sendMessage("§7등록된 스킬이 없습니다.");
                return true;
            }
            for (String s : skills) {
                SkillMeta sm = SkillMeta.loadFromConfig(s, (JavaPlugin) plugin);
                String disp = sm != null ? sm.display : s;
                player.sendMessage(" - §6" + s + "§f ( " + disp + " )");
            }
            return true;
        }

        if (sub.equals("info")) {
            if (args.length < 2) {
                player.sendMessage("§e사용법: /skill info <스킬키>");
                return true;
            }
            String skill = args[1];
            SkillMeta sm = SkillMeta.loadFromConfig(skill, (JavaPlugin) plugin);
            if (sm == null) {
                player.sendMessage("§c해당 스킬을 찾을 수 없습니다: " + skill);
                return true;
            }
            int level = skillManager.getSkillLevel(player, skill);
            int exp = skillManager.getSkillExp(player, skill);
            int nextExp = sm.getExpForLevel(level + 1);
            player.sendMessage("§a스킬 정보: §6" + sm.display + "§f (" + skill + ")");
            player.sendMessage(" - 레벨: §b" + level);
            player.sendMessage(" - 경험치: §b" + exp + "§f / 다음 레벨 필요: §b" + nextExp);
            player.sendMessage(" - 쿨다운(현재 레벨 기준): §b" + sm.getCooldown(level) + "초");
            String eff = sm.levelEffects.getOrDefault(level, "");
            player.sendMessage(" - 효과: §7" + (eff.isEmpty() ? "없음" : eff));
            return true;
        }

        if (sub.equals("use")) {
            if (args.length < 2) {
                player.sendMessage("§e사용법: /skill use <스킬키>");
                return true;
            }
            String skill = args[1];
            boolean ok = skillManager.useSkill(player, skill);
            if (!ok) player.sendMessage("§c스킬 사용에 실패했습니다.");
            return true;
        }

        // fallback: help
        player.sendMessage("§e스킬 명령어:");
        player.sendMessage(" /skill - 스킬 트리 열기");
        player.sendMessage(" /skill list - 장착된 직업의 스킬 목록");
        player.sendMessage(" /skill info <스킬키> - 스킬 상세 정보");
        player.sendMessage(" /skill use <스킬키> - 스킬 사용");
        return true;
    }
}