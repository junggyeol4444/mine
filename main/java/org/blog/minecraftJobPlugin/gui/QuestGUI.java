package org.blog.minecraftJobPlugin.gui;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.quest.QuestManager;
import org.blog.minecraftJobPlugin.quest.QuestMeta;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Quest GUI - 퀘스트 목록 및 제출
 */
public class QuestGUI {

    private static final String TITLE_PREFIX = "퀘스트: ";

    private final JobPlugin plugin;
    private final Player player;
    private final QuestManager questManager;
    private final JobManager jobManager;
    private final PluginDataUtil dataUtil;

    public QuestGUI(JobPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.questManager = plugin.getQuestManager();
        this.jobManager = plugin.getJobManager();
        this.dataUtil = new PluginDataUtil(plugin);
    }

    public void open() {
        String job = jobManager.getActiveJob(player);
        if (job == null) {
            player.sendMessage("§c장착된 직업이 없습니다. 먼저 직업을 선택해 주세요.");
            return;
        }

        List<QuestMeta> quests = questManager.getJobQuests(job);
        if (quests.isEmpty()) {
            player.sendMessage("§e현재 직업(" + job + ")의 퀘스트가 존재하지 않습니다.");
            return;
        }

        int size = 9;
        while (size < quests.size()) size += 9;
        Inventory inv = Bukkit.createInventory(null, size, TITLE_PREFIX + job);

        for (int i = 0; i < quests.size(); i++) {
            QuestMeta qm = quests.get(i);
            int prog = questManager.getProgress(player, qm.id);
            int target = questManager.getQuestObjective(qm.id, "amount", 1);
            boolean completed = prog >= target;
            
            ItemStack item = new ItemStack(completed ? Material.LIME_DYE : Material.PAPER);
            ItemMeta im = item.getItemMeta();
            
            String title = (completed ? ChatColor.GREEN + "§l" : ChatColor.GOLD) + qm.name;
            im.setDisplayName(title);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "ID: " + qm.id);
            lore.add(ChatColor.YELLOW + "보상: " + qm.reward + plugin.getConfig().getString("economy.currency_symbol", "원") + " + 특성 포인트 1");
            lore.add(ChatColor.AQUA + "진행도: " + prog + " / " + target + 
                    (completed ? " §a✓" : ""));
            lore.add("");
            
            if (completed) {
                lore.add(ChatColor.GREEN + "§l우클릭: 퀘스트 제출");
            } else {
                lore.add(ChatColor.GRAY + "좌클릭: 상세보기");
            }
            
            im.setLore(lore);
            item.setItemMeta(im);
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    public static boolean isQuestInventory(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public static String getQuestIdFromSlot(JobPlugin plugin, Player player, int slot) {
        String job = plugin.getJobManager().getActiveJob(player);
        if (job == null) return null;
        
        List<QuestMeta> quests = plugin.getQuestManager().getJobQuests(job);
        if (slot >= 0 && slot < quests.size()) {
            return quests.get(slot).id;
        }
        return null;
    }
}