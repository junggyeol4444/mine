package com.yourname.jobplugin.gui;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.quest.QuestManager;
import com.yourname.jobplugin.quest.QuestMeta;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class QuestGUI {

    private final JobPlugin plugin;
    private final Player player;

    public QuestGUI(JobPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        String job = plugin.getJobManager().getActiveJob(player);
        if (job == null) {
            player.sendMessage("§c직업 장착 필요");
            return;
        }
        QuestManager qm = plugin.getQuestManager();
        List<QuestMeta> quests = qm.getJobQuests(job);
        Inventory inv = Bukkit.createInventory(player, 27, "직업 전용 퀘스트");

        for (int i = 0; i < quests.size() && i < 27; i++) {
            QuestMeta q = quests.get(i);
            ItemStack icon = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName("§e" + q.name);
            im.setLore(List.of(
                    "§f타입: " + q.type,
                    "§a보상: " + q.reward,
                    "§7진행도: (확장 구현 필요)"
            ));

            icon.setItemMeta(im);
            inv.setItem(i, icon);
        }
        player.openInventory(inv);

        // 클릭 시 진행/완료/설명 팝업 등 추가 리스너 구현
    }
}