package org.blog.minecraftJobPlugin.listener;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.gui.JobSelectionGUI;
import org.blog.minecraftJobPlugin.gui.ShopGUI;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class JobGuiListener implements Listener {

    private final JobPlugin plugin;

    public JobGuiListener(JobPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() == null) return;
        String title = e.getView().getTitle();
        Player p = (Player) e.getWhoClicked();

        if (JobSelectionGUI.isJobInventory(title)) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String display = clicked.getItemMeta().getDisplayName();
            if (display == null) return;
            JobManager jm = plugin.getJobManager();
            String jobKey = jm.getAllJobs().stream().filter(meta -> (ChatColor.AQUA + meta.display).equals(display) || meta.display.equals(ChatColor.stripColor(display))).map(meta->meta.key).findFirst().orElse(null);
            if (jobKey == null) {
                p.sendMessage("§c직업을 확인할 수 없습니다.");
                return;
            }
            if (e.isLeftClick()) {
                jm.addJob(p, jobKey);
                p.sendMessage("§a직업을 획득했습니다: §b" + jobKey);
            } else if (e.isRightClick()) {
                if (!jm.hasJob(p, jobKey)) {
                    p.sendMessage("§c해당 직업을 보유하고 있지 않습니다. 먼저 획득하세요.");
                } else {
                    jm.setActiveJob(p, jobKey);
                    p.sendMessage("§a장착 직업 변경: §b" + jobKey);
                }
            }
            return;
        }

        if (ShopGUI.isShopInventory(title)) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String job = title.replace(ShopGUI.SHOP_TITLE_PREFIX, "");
            if (clicked.getType() == Material.EMERALD && clicked.getItemMeta().getDisplayName().contains("판매하기")) {
                ShopGUI shop = new ShopGUI(plugin, p, job);
                shop.openSell();
                return;
            }
            ShopGUI shop = new ShopGUI(plugin, p, job);
            boolean ok = shop.handlePurchase(clicked);
            if (ok) p.closeInventory();
            return;
        }

        if (ShopGUI.isSellInventory(title)) {
            boolean handled = ShopGUI.handleSellClick(e, plugin);
            if (handled) return;
        }
    }
}