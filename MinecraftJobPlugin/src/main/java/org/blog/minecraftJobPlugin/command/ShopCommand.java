package com.yourname.jobplugin.command;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.job.JobManager;
import com.yourname.jobplugin.gui.ShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final JobManager jobManager;

    public ShopCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("[상점] 게임 내에서만 사용 가능합니다.");
            return true;
        }
        Player player = (Player)sender;

        String activeJob = jobManager.getActiveJob(player);

        if (activeJob == null) {
            player.sendMessage("§c직업을 장착한 후 상점을 이용할 수 있습니다.");
            return true;
        }

        ShopGUI gui = new ShopGUI(plugin, player, activeJob);
        gui.open();

        return true;
    }
}