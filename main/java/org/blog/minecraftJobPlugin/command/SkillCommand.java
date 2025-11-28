package org.blog.minecraftJobPlugin.command;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.job.JobMeta;
import org.blog.minecraftJobPlugin.skill.SkillManager;
import org.blog.minecraftJobPlugin.gui.SkillTreeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /skill 명령어
 * - /skill - 스킬 트리 GUI 열기
 * - /skill list - 내 스킬 목록 보기
 * - /skill use <스킬명> - 스킬 사용
 * - /skill info <스킬명> - 스킬 정보 보기
 */
public class SkillCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final SkillManager skillManager;

    public SkillCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.skillManager = plugin.getSkillManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c게임 내에서만 사용 가능합니다.");
            return true;
        }

        Player player = (Player) sender;
        String activeJob = jobManager.getActiveJob(player);

        if (activeJob == null) {
            player.sendMessage("§c직업을 먼저 장착해주세요. /job 으로 직업을 선택하세요.");
            return true;
        }

        // /skill - GUI 열기
        if (args.length == 0) {
            new SkillTreeGUI(plugin, player).open();
            return true;
        }

        // /skill list - 스킬 목록
        if (args[0].equalsIgnoreCase("list")) {
            JobMeta meta = jobManager.getJobMeta(activeJob);
            if (meta == null || meta.skills.isEmpty()) {
                player.sendMessage("§c현재 직업에 스킬이 없습니다.");
                return true;
            }

            player.sendMessage("§a━━━━━━ " + meta.display + " 스킬 목록 ━━━━━━");
            for (String skillKey : meta.skills) {
                int level = skillManager.getSkillLevel(player, skillKey);
                int exp = skillManager.getSkillExp(player, skillKey);
                String displayName = skillManager.getSkillMeta(skillKey).display;
                player.sendMessage("§e" + displayName + " §7(Lv." + level + ") §f- 경험치: " + exp);
            }
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        // /skill use <스킬명> - 스킬 사용
        if (args[0].equalsIgnoreCase("use")) {
            if (args.length < 2) {
                player.sendMessage("§c사용법: /skill use <스킬명>");
                return true;
            }

            String skillName = args[1];
            JobMeta meta = jobManager.getJobMeta(activeJob);
            
            if (meta == null || !meta.skills.contains(skillName)) {
                player.sendMessage("§c해당 스킬을 보유하지 않았습니다.");
                player.sendMessage("§e사용 가능한 스킬: " + String.join(", ", meta.skills));
                return true;
            }

            boolean success = skillManager.useSkill(player, skillName);
            if (!success) {
                // 쿨타임 메시지는 SkillManager에서 출력
            }
            return true;
        }

        // /skill info <스킬명> - 스킬 정보
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                player.sendMessage("§c사용법: /skill info <스킬명>");
                return true;
            }

            String skillName = args[1];
            JobMeta meta = jobManager.getJobMeta(activeJob);
            
            if (meta == null || !meta.skills.contains(skillName)) {
                player.sendMessage("§c해당 스킬을 보유하지 않았습니다.");
                return true;
            }

            var skillMeta = skillManager.getSkillMeta(skillName);
            int level = skillManager.getSkillLevel(player, skillName);
            int exp = skillManager.getSkillExp(player, skillName);
            int nextLevelExp = skillMeta.getExpForLevel(level + 1);
            int cooldown = skillMeta.getCooldown(level);

            player.sendMessage("§a━━━━━━ 스킬 정보 ━━━━━━");
            player.sendMessage("§6스킬명: §e" + skillMeta.display);
            player.sendMessage("§6레벨: §e" + level);
            player.sendMessage("§6경험치: §e" + exp + " / " + nextLevelExp);
            player.sendMessage("§6쿨타임: §e" + cooldown + "초");
            player.sendMessage("§6현재 효과: §f" + skillMeta.levelEffects.getOrDefault(level, "없음"));
            if (level < 10) {
                player.sendMessage("§6다음 레벨 효과: §f" + skillMeta.levelEffects.getOrDefault(level + 1, "최대 레벨"));
            }
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━");
            return true;
        }

        player.sendMessage("§e사용법:");
        player.sendMessage("§f/skill §7- 스킬 트리 GUI");
        player.sendMessage("§f/skill list §7- 스킬 목록");
        player.sendMessage("§f/skill use <스킬명> §7- 스킬 사용");
        player.sendMessage("§f/skill info <스킬명> §7- 스킬 정보");
        return true;
    }
}