package org.blog.minecraftJobPlugin.listener;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.gui.JobSelectionGUI;
import org.blog.minecraftJobPlugin.gui.ShopGUI;
import org.blog.minecraftJobPlugin.gui.SkillTreeGUI;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.skill.TraitMeta;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
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

        // 직업 선택 GUI
        if (JobSelectionGUI.isJobInventory(title)) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String display = clicked.getItemMeta().getDisplayName();
            if (display == null) return;
            JobManager jm = plugin.getJobManager();
            String jobKey = jm.getAllJobs().stream()
                .filter(meta -> (ChatColor.AQUA + meta.display).equals(display) || meta.display.equals(ChatColor.stripColor(display)))
                .map(meta->meta.key)
                .findFirst()
                .orElse(null);
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

        // 상점 GUI
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

        // 판매 GUI
        if (ShopGUI.isSellInventory(title)) {
            boolean handled = ShopGUI.handleSellClick(e, plugin);
            if (handled) return;
        }

        // 스킬 트리 GUI
        if (SkillTreeGUI.isSkillTreeInventory(title)) {
            int slot = e.getRawSlot();

            // 스킬 섹션 (0~8)
            if (slot >= 0 && slot < 9) {
                e.setCancelled(true);
                String skillName = SkillTreeGUI.getSkillNameFromSlot(plugin, p, slot);
                if (skillName == null) return;

                // 좌클릭: 스킬 사용
                if (e.getClick() == ClickType.LEFT) {
                    plugin.getSkillManager().useSkill(p, skillName);
                }
                // 우클릭: 스킬북 받기
                else if (e.getClick() == ClickType.RIGHT) {
                    SkillItemListener listener = new SkillItemListener(plugin);
                    String displayName = plugin.getSkillManager().getSkillMeta(skillName).display;
                    ItemStack skillBook = listener.createSkillBook(skillName, displayName, "스킬을 빠르게 사용할 수 있는 책");
                    p.getInventory().addItem(skillBook);
                    p.sendMessage("§a스킬북을 받았습니다: §e" + displayName);
                    p.closeInventory();
                }
                return;
            }

            // 특성 섹션 (18~35)
            if (slot >= 18 && slot < 36) {
                e.setCancelled(true);
                TraitMeta trait = SkillTreeGUI.getTraitFromSlot(plugin, p, slot);
                if (trait == null) return;

                String job = plugin.getJobManager().getActiveJob(p);
                if (job == null) return;

                boolean success = plugin.getTraitManager().selectTrait(p, job, trait.name);
                if (success) {
                    p.closeInventory();
                    // GUI 다시 열기
                    new SkillTreeGUI(plugin, p).open();
                }
                return;
            }

            // 나머지는 취소
            e.setCancelled(true);
        }

        // 퀘스트 GUI
        if (org.blog.minecraftJobPlugin.gui.QuestGUI.isQuestInventory(title)) {
            e.setCancelled(true);
            int slot = e.getRawSlot();

            String questId = org.blog.minecraftJobPlugin.gui.QuestGUI.getQuestIdFromSlot(plugin, p, slot);
            if (questId == null) return;

            // 우클릭: 퀘스트 제출
            if (e.getClick() == ClickType.RIGHT) {
                plugin.getQuestManager().submitQuest(p, questId);
                p.closeInventory();
                // GUI 다시 열기
                new org.blog.minecraftJobPlugin.gui.QuestGUI(plugin, p).open();
            }
            // 좌클릭: 상세 정보
            else if (e.getClick() == ClickType.LEFT) {
                int prog = plugin.getQuestManager().getProgress(p, questId);
                int target = plugin.getQuestManager().getQuestObjective(questId, "amount", 1);
                p.sendMessage("§a━━━━━━ 퀘스트 정보 ━━━━━━");
                p.sendMessage("§6ID: §e" + questId);
                p.sendMessage("§6진행도: §e" + prog + " / " + target);
                if (prog >= target) {
                    p.sendMessage("§a완료됨! 우클릭으로 제출하세요.");
                } else {
                    p.sendMessage("§7목표를 달성하면 제출할 수 있습니다.");
                }
                p.sendMessage("§a━━━━━━━━━━━━━━━━━━");
            }
        }
    }
}