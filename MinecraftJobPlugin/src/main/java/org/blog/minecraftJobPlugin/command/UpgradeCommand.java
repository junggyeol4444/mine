package org.blog.minecraftJobPlugin.command;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.equipment.EquipmentManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /upgrade  -> 손에 든 아이템 업그레이드(비용 차감)
 */
public class UpgradeCommand implements CommandExecutor {

    private final JobPlugin plugin;
    private final EquipmentManager equipmentManager;

    public UpgradeCommand(JobPlugin plugin) {
        this.plugin = plugin;
        this.equipmentManager = plugin.getEquipmentManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("콘솔에서 사용할 수 없는 명령어입니다.");
            return true;
        }
        Player p = (Player) sender;
        boolean ok = equipmentManager.upgradeItemInHand(p);
        if (!ok) {
            p.sendMessage("§c업그레이드에 실패했습니다.");
        }
        return true;
    }
}