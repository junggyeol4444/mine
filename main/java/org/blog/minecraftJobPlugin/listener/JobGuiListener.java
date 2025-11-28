package org.blog.minecraftJobPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.Job;
import org.blog.minecraftJobPlugin.manager.JobManager;
import org.blog.minecraftJobPlugin.economy.EconomyManager;

import java.util.List;

public class JobGuiListener implements Listener {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final EconomyManager economyManager;

    public JobGuiListener(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.economyManager = plugin.getEconomyManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) {
            return;
        }

        String title = event.getView().getTitle();

        // 직업 선택 GUI
        if (title.equals("§6§l직업 선택")) {
            event.setCancelled(true);
            handleJobSelectionClick(player, event);
        }
        // 상점 GUI
        else if (title.startsWith("§6§l")) {
            String jobName = ChatColor.stripColor(title).replace("상점", "").trim();
            if (jobManager.getJob(jobName) != null) {
                event.setCancelled(true);
                handleShopClick(player, event, jobName);
            }
        }
        // 스킬 트리 GUI
        else if (title.equals("§6§l스킬 트리")) {
            event.setCancelled(true);
            handleSkillTreeClick(player, event);
        }
        // 퀘스트 GUI
        else if (title.equals("§6§l퀘스트")) {
            event.setCancelled(true);
            handleQuestClick(player, event);
        }
    }

    /**
     * 직업 선택 GUI 클릭 처리
     */
    private void handleJobSelectionClick(Player player, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        String displayName = ChatColor.stripColor(meta.getDisplayName());
        Job job = jobManager.getJob(displayName);

        if (job == null) {
            return;
        }

        // 좌클릭: 직업 획득
        if (event.isLeftClick()) {
            acquireJob(player, job);
        }
        // 우클릭: 직업 장착/해제
        else if (event.isRightClick()) {
            toggleJobEquip(player, job);
        }
    }

    /**
     * 직업 획득 (중복 방지 + 비용 차감)
     */
    private void acquireJob(Player player, Job job) {
        String jobName = job.getName();

        // 1. 이미 보유한 직업인지 확인
        if (jobManager.hasJob(player, jobName)) {
            String message = plugin.getConfig().getString("messages.job_already_owned", "§c이미 보유한 직업입니다!");
            player.sendMessage(message);
            player.closeInventory();
            return;
        }

        // 2. 최대 직업 개수 확인
        int maxJobs = plugin.getConfig().getInt("job.max_jobs_per_player", -1);
        if (maxJobs > 0) {
            List<String> ownedJobs = jobManager.getPlayerJobs(player);
            if (ownedJobs.size() >= maxJobs) {
                String message = plugin.getConfig().getString("messages.job_max_reached",
                                "§c최대 직업 개수에 도달했습니다!")
                        .replace("%max%", String.valueOf(maxJobs));
                player.sendMessage(message);
                player.closeInventory();
                return;
            }
        }

        // 3. 직업 획득 비용 계산
        double cost = calculateJobCost(player, jobName);

        // 4. 잔액 확인
        double balance = economyManager.getBalance(player.getUniqueId());
        if (balance < cost) {
            String currency = plugin.getConfig().getString("economy.currency_symbol", "원");
            String message = plugin.getConfig().getString("messages.job_insufficient_funds",
                            "§c직업을 획득하기 위한 돈이 부족합니다!")
                    .replace("%cost%", String.valueOf((int)cost))
                    .replace("%currency%", currency);
            player.sendMessage(message);
            player.closeInventory();
            return;
        }

        // 5. 비용 차감
        if (cost > 0) {
            economyManager.withdraw(player.getUniqueId(), cost);
        }

        // 6. 직업 획득
        jobManager.addJob(player, jobName);

        // 7. 메시지 전송
        String currency = plugin.getConfig().getString("economy.currency_symbol", "원");
        String message;

        if (cost > 0) {
            message = plugin.getConfig().getString("messages.job_acquired_paid",
                            "§a직업을 획득했습니다: §b%job% §7(비용: §e%cost%%currency%§7)")
                    .replace("%job%", jobName)
                    .replace("%cost%", String.valueOf((int)cost))
                    .replace("%currency%", currency);
        } else {
            message = plugin.getConfig().getString("messages.job_acquired",
                            "§a직업을 획득했습니다: §b%job%")
                    .replace("%job%", jobName);
        }

        player.sendMessage(message);

        // 8. 시작 아이템 지급
        giveStartingItems(player, job);

        // 9. GUI 새로고침
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openJobSelectionGui(player);
        }, 2L);
    }

    /**
     * 직업 획득 비용 계산
     */
    private double calculateJobCost(Player player, String jobName) {
        // 특정 직업 비용이 설정되어 있는지 확인
        if (plugin.getConfig().contains("job.job_specific_costs." + jobName)) {
            return plugin.getConfig().getDouble("job.job_specific_costs." + jobName);
        }

        // 보유 직업 개수에 따른 비용
        int ownedJobCount = jobManager.getPlayerJobs(player).size();

        switch (ownedJobCount) {
            case 0:
                return plugin.getConfig().getDouble("job.first_job_cost", 0);
            case 1:
                return plugin.getConfig().getDouble("job.second_job_cost", 50000);
            case 2:
                return plugin.getConfig().getDouble("job.third_job_cost", 100000);
            default:
                return plugin.getConfig().getDouble("job.additional_job_cost", 200000);
        }
    }

    /**
     * 직업 장착/해제
     */
    private void toggleJobEquip(Player player, Job job) {
        String jobName = job.getName();

        // 직업을 보유하고 있지 않으면 장착 불가
        if (!jobManager.hasJob(player, jobName)) {
            player.sendMessage("§c먼저 좌클릭으로 직업을 획득해야 합니다!");
            return;
        }

        // 현재 장착된 직업 확인
        String activeJob = jobManager.getActiveJob(player);

        if (jobName.equals(activeJob)) {
            // 같은 직업 클릭 시 해제
            jobManager.setActiveJob(player, null);
            player.sendMessage("§c직업 장착을 해제했습니다.");
        } else {
            // 다른 직업 장착
            jobManager.setActiveJob(player, jobName);
            String message = plugin.getConfig().getString("messages.job_switched",
                            "§a장착 직업 변경: §b%job%")
                    .replace("%job%", jobName);
            player.sendMessage(message);
        }

        // GUI 새로고침
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openJobSelectionGui(player);
        }, 2L);
    }

    /**
     * 시작 아이템 지급
     */
    private void giveStartingItems(Player player, Job job) {
        List<ItemStack> startingItems = job.getStartingItems();
        if (startingItems == null || startingItems.isEmpty()) {
            return;
        }

        for (ItemStack item : startingItems) {
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item.clone());
            }
        }

        player.sendMessage("§a시작 아이템을 지급받았습니다!");
    }

    /**
     * 직업 선택 GUI 열기
     */
    public void openJobSelectionGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§l직업 선택");

        List<Job> allJobs = jobManager.getAllJobs();
        List<String> ownedJobs = jobManager.getPlayerJobs(player);
        String activeJob = jobManager.getActiveJob(player);

        int slot = 10;
        for (Job job : allJobs) {
            ItemStack icon = job.getIcon().clone();
            ItemMeta meta = icon.getItemMeta();

            if (meta != null) {
                String displayName = meta.getDisplayName();
                List<String> lore = meta.getLore();

                // 보유 여부 및 장착 상태 표시
                boolean owned = ownedJobs.contains(job.getName());
                boolean active = job.getName().equals(activeJob);

                if (owned) {
                    if (active) {
                        displayName += " §a§l[장착중]";
                    } else {
                        displayName += " §7[보유]";
                    }
                } else {
                    displayName += " §c[미보유]";
                }

                meta.setDisplayName(displayName);

                // 비용 정보 추가
                if (!owned) {
                    double cost = calculateJobCost(player, job.getName());
                    String currency = plugin.getConfig().getString("economy.currency_symbol", "원");

                    if (lore != null) {
                        lore.add("");
                        if (cost > 0) {
                            lore.add("§e획득 비용: §6" + (int)cost + currency);
                        } else {
                            lore.add("§a무료로 획득 가능!");
                        }
                        lore.add("");
                        lore.add("§7좌클릭: §f직업 획득");
                    }
                } else {
                    if (lore != null) {
                        lore.add("");
                        lore.add("§7우클릭: §f장착/해제");
                    }
                }

                meta.setLore(lore);
                icon.setItemMeta(meta);
            }

            gui.setItem(slot, icon);

            slot++;
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
        }

        // 정보 아이템 추가
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6§l직업 시스템 안내");
            infoMeta.setLore(List.of(
                    "§7━━━━━━━━━━━━━━━━━━━━",
                    "§f좌클릭: §a직업 획득",
                    "§f우클릭: §a직업 장착/해제",
                    "§7━━━━━━━━━━━━━━━━━━━━",
                    "§e보유 직업: §f" + ownedJobs.size() + "개",
                    "§e장착 직업: §f" + (activeJob != null ? activeJob : "없음"),
                    "§7━━━━━━━━━━━━━━━━━━━━"
            ));
            info.setItemMeta(infoMeta);
        }
        gui.setItem(49, info);

        player.openInventory(gui);
    }

    /**
     * 상점 GUI 클릭 처리
     */
    private void handleShopClick(Player player, InventoryClickEvent event, String jobName) {
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 상점 아이템 구매/판매 로직
        // TODO: ShopManager를 통한 구매/판매 처리
        player.sendMessage("§e상점 기능은 아직 구현 중입니다.");
    }

    /**
     * 스킬 트리 GUI 클릭 처리
     */
    private void handleSkillTreeClick(Player player, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 스킬 학습 로직
        // TODO: SkillManager를 통한 스킬 학습
        player.sendMessage("§e스킬 시스템은 아직 구현 중입니다.");
    }

    /**
     * 퀘스트 GUI 클릭 처리
     */
    private void handleQuestClick(Player player, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 퀘스트 완료/보상 로직
        // TODO: QuestManager를 통한 퀘스트 처리
        player.sendMessage("§e퀘스트 시스템은 아직 구현 중입니다.");
    }
}