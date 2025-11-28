package org.blog.minecraftJobPlugin.command;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.gui.QuestGUI;
import org.blog.minecraftJobPlugin.manager.JobManager;
import org.blog.minecraftJobPlugin.quest.QuestManager;
import org.blog.minecraftJobPlugin.quest.QuestMeta;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /quest 명령어
 * - /quest - 퀘스트 GUI 열기
 * - /quest list - 퀘스트 목록 (텍스트)
 * - /quest submit <퀘스트ID> - 퀘스트 제출
 * - /quest progress - 진행 중인 퀘스트 확인
 */
public class QuestCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final QuestManager questManager;
    private final PluginDataUtil dataUtil;

    public QuestCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.questManager = plugin.getQuestManager();
        this.dataUtil = new PluginDataUtil(plugin);
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

        // /quest - GUI 열기
        if (args.length == 0) {
            new QuestGUI(plugin, player).open();
            return true;
        }

        // /quest list - 퀘스트 목록
        if (args[0].equalsIgnoreCase("list")) {
            List<QuestMeta> quests = questManager.getJobQuests(activeJob);
            if (quests.isEmpty()) {
                player.sendMessage("§e현재 직업(" + activeJob + ")의 퀘스트가 없습니다.");
                return true;
            }

            player.sendMessage("§a━━━━━━ " + activeJob + " 퀘스트 ━━━━━━");
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(player.getUniqueId());

            for (QuestMeta quest : quests) {
                int progress = cfg.getInt("quests." + quest.id + ".progress", 0);
                player.sendMessage("§6[" + quest.id + "] §e" + quest.name);
                player.sendMessage("  §7진행도: §f" + progress + " §7| 보상: §e" + quest.reward + "원");
            }
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        // /quest submit <ID> - 퀘스트 제출
        if (args[0].equalsIgnoreCase("submit")) {
            if (args.length < 2) {
                player.sendMessage("§c사용법: /quest submit <퀘스트ID>");
                return true;
            }

            String questId = args[1];
            questManager.submitQuest(player, questId);
            return true;
        }

        // /quest progress - 진행 중인 퀘스트
        if (args[0].equalsIgnoreCase("progress")) {
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(player.getUniqueId());
            List<QuestMeta> quests = questManager.getJobQuests(activeJob);

            player.sendMessage("§a━━━━━━ 진행 중인 퀘스트 ━━━━━━");
            boolean hasProgress = false;

            for (QuestMeta quest : quests) {
                int progress = cfg.getInt("quests." + quest.id + ".progress", 0);
                if (progress > 0) {
                    hasProgress = true;
                    player.sendMessage("§6" + quest.name + " §7- 진행: §e" + progress);
                }
            }

            if (!hasProgress) {
                player.sendMessage("§7진행 중인 퀘스트가 없습니다.");
            }
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        player.sendMessage("§e사용법:");
        player.sendMessage("§f/quest §7- 퀘스트 GUI");
        player.sendMessage("§f/quest list §7- 퀘스트 목록");
        player.sendMessage("§f/quest submit <ID> §7- 퀘스트 제출");
        player.sendMessage("§f/quest progress §7- 진행도 확인");
        return true;
    }
}