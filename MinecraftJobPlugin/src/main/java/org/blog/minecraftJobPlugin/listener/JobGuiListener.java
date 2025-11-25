package com.yourname.jobplugin.listener;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.job.JobManager;
import com.yourname.jobplugin.skill.TraitManager;
import com.yourname.jobplugin.gui.JobSelectionGUI;
import com.yourname.jobplugin.gui.SkillTreeGUI;
import com.yourname.jobplugin.gui.QuestGUI;
import com.yourname.jobplugin.gui.ShopGUI;
import com.yourname.jobplugin.econ.EconomyManager;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class JobGuiListener implements Listener {
    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final TraitManager traitManager;
    private final EconomyManager economyManager;

    public JobGuiListener(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.traitManager = plugin.getTraitManager();
        this.economyManager = plugin.getEconomyManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        // 직업 선택 GUI
        if (title.contains("직업 선택")) {
            event.setCancelled(true);
            String jobClicked = parseJobFromItem(clicked);
            if (jobClicked != null && jobManager.hasJob(player, jobClicked)) {
                jobManager.setActiveJob(player, jobClicked);
                player.sendMessage("§a[" + jobClicked + "] 직업 장착 완료!");
                player.closeInventory();
            } else {
                player.sendMessage("§c해당 직업 미보유 또는 선택 불가.");
            }
        }

        // 특성 트리 GUI
        if (title.contains("스킬 트리")) {
            event.setCancelled(true);
            String traitClicked = parseTraitFromItem(clicked);
            String job = jobManager.getActiveJob(player);
            if (traitClicked != null && traitManager.getPoints(player, job) > 0) {
                traitManager.selectTrait(player, job, traitClicked);
                player.sendMessage("§a특성 [" + traitClicked + "] 선택!");
                player.closeInventory();
            } else {
                player.sendMessage("§c포인트 부족 또는 선택 불가.");
            }
        }

        // 퀘스트 GUI
        if (title.contains("직업 전용 퀘스트")) {
            event.setCancelled(true);
            // 퀘스트 완료/수락/보상
            // TODO: 퀘스트 선택 및 완료 체크, 보상 지급 등
            player.sendMessage("§e퀘스트 기능은 확장 구현 필요.");
        }

        // 상점 GUI
        if (title.contains("상점")) {
            event.setCancelled(true);
            // 아이템 구매: 금액 체크, item 지급, 이코노미 연동
            int price = parsePriceFromItem(clicked);
            if (economyManager.takeMoney(player, price)) {
                player.getInventory().addItem(clicked.clone());
                player.sendMessage("§a상점 아이템 구매 성공!");
            } else {
                player.sendMessage("§c금액 부족!");
            }
        }
    }

    private String parseJobFromItem(ItemStack item) {
        if (item != null && item.getItemMeta() != null
                && item.getItemMeta().getDisplayName() != null) {
            return item.getItemMeta().getDisplayName().replace("§e", "");
        }
        return null;
    }
    private String parseTraitFromItem(ItemStack item) {
        if (item != null && item.getItemMeta() != null
                && item.getItemMeta().getDisplayName() != null) {
            return item.getItemMeta().getDisplayName().replace("§b", "");
        }
        return null;
    }
    private int parsePriceFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null || item.getItemMeta().getLore() == null) return 0;
        for (String lore : item.getItemMeta().getLore())
            if (lore.contains("가격:"))
                return Integer.parseInt(lore.replaceAll("[^0-9]", ""));
        return 0;
    }
}