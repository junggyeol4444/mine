package org.blog.minecraftJobPlugin.command;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobComboManager;
import org.blog.minecraftJobPlugin.manager.JobManager;  // job → manager
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class ComboCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final JobComboManager comboManager;
    private final PluginDataUtil dataUtil;

    public ComboCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.comboManager = plugin.getComboManager();
        this.dataUtil = new PluginDataUtil(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c게임 내에서만 사용 가능합니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            Set<String> myCombos = comboManager.getUnlockedCombos(player);

            if (myCombos.isEmpty()) {
                player.sendMessage("§e해금된 조합이 없습니다.");
                player.sendMessage("§7여러 직업을 획득하면 조합을 해금할 수 있습니다!");
                return true;
            }

            player.sendMessage("§a━━━━━━ 내 직업 조합 ━━━━━━");
            YamlConfiguration combos = dataUtil.loadGlobal("combos");

            for (String comboKey : myCombos) {
                String unlock = combos.getString("combos." + comboKey + ".unlock", "조합 효과");
                player.sendMessage("§6" + comboKey + " §f- " + unlock);
            }
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            List<String> allCombos = comboManager.getAllCombos();
            Set<String> unlocked = comboManager.getUnlockedCombos(player);

            player.sendMessage("§a━━━━━━ 전체 직업 조합 ━━━━━━");
            YamlConfiguration combos = dataUtil.loadGlobal("combos");

            for (String comboKey : allCombos) {
                boolean hasIt = unlocked.contains(comboKey);
                String unlock = combos.getString("combos." + comboKey + ".unlock", "조합 효과");
                List<String> requirements = combos.getStringList("combos." + comboKey + ".requirements");

                String status = hasIt ? "§a✓ " : "§7✗ ";
                player.sendMessage(status + "§6" + comboKey);
                player.sendMessage("  §f" + unlock);
                player.sendMessage("  §7필요 직업: §e" + String.join(", ", requirements));
            }
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            comboManager.checkAndUnlockCombos(player);
            player.sendMessage("§a조합 확인 완료!");
            return true;
        }

        player.sendMessage("§e사용법:");
        player.sendMessage("§f/combo §7- 내 조합 목록");
        player.sendMessage("§f/combo list §7- 모든 조합 보기");
        player.sendMessage("§f/combo check §7- 해금 가능 확인");
        return true;
    }
}