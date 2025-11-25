package com.yourname.jobplugin.gui;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.job.JobMeta;
import com.yourname.jobplugin.job.JobManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class JobSelectionGUI {
    private final Player player;
    private final JobManager jobManager;

    public JobSelectionGUI(JobPlugin plugin, Player player) {
        this.player = player;
        this.jobManager = plugin.getJobManager();
    }

    public void open() {
        List<JobMeta> jobs = jobManager.getAllJobs();
        int size = ((jobs.size() - 1) / 9 + 1) * 9;
        Inventory inv = Bukkit.createInventory(player, size, "직업 선택");

        for (int i = 0; i < jobs.size(); i++) {
            JobMeta meta = jobs.get(i);
            ItemStack icon = getIcon(meta.id);
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName("§e" + meta.display);
            List<String> lore = new ArrayList<>();
            lore.add("§7" + meta.description);
            lore.add("§f스킬: " + String.join(", ", meta.skills));
            lore.add("§b행동 제한: " + String.join(", ", meta.actions));
            if (jobManager.hasJob(player, meta.id)) {
                lore.add("§a보유중 (클릭 시 장착)");
            } else {
                lore.add("§c미보유");
            }
            im.setLore(lore);
            icon.setItemMeta(im);

            inv.setItem(i, icon);
        }

        player.openInventory(inv);
        // 이벤트: 인벤토리 클릭 시 해당 직업 장착(별도 리스너에서 구현)
    }

    private ItemStack getIcon(String job) {
        // 대표 아이콘 지정
        switch (job) {
            case "explorer": return new ItemStack(Material.BOOK);
            case "carpenter": return new ItemStack(Material.IRON_AXE);
            case "miner": return new ItemStack(Material.IRON_PICKAXE);
            case "hunter": return new ItemStack(Material.BOW);
            case "farmer": return new ItemStack(Material.WHEAT_SEEDS);
            case "fisher": return new ItemStack(Material.FISHING_ROD);
            case "blacksmith": return new ItemStack(Material.ANVIL);
            case "alchemist": return new ItemStack(Material.POTION);
            case "chef": return new ItemStack(Material.BREAD);
            default: return new ItemStack(Material.PAPER);
        }
    }
}