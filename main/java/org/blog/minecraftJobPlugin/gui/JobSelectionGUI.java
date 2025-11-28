package org.blog.minecraftJobPlugin.gui;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobMeta;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class JobSelectionGUI {

    private final JobPlugin plugin;
    private final Player player;
    private final JobManager jobManager;

    private static final String TITLE = "직업 선택";

    public JobSelectionGUI(JobPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.jobManager = plugin.getJobManager();
    }

    public void open() {
        List<JobMeta> jobs = jobManager.getAllJobs();
        int size = 9;
        while (size < jobs.size()) size += 9;
        Inventory inv = Bukkit.createInventory(null, size, TITLE);
        for (int i = 0; i < jobs.size(); i++) {
            JobMeta jm = jobs.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + jm.display);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + jm.description);
            lore.add("");
            lore.add(ChatColor.YELLOW + "좌클릭: 직업 획득 / 우클릭: 장착");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        player.openInventory(inv);
    }

    public static boolean isJobInventory(String title) {
        return TITLE.equals(title);
    }
}