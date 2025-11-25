package com.yourname.jobplugin.listener;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.job.JobManager;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.Bukkit;

public class JobEventListener implements Listener {

    private final JobPlugin plugin;
    private final JobManager jobManager;

    public JobEventListener(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    // 광물 채굴 제한
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String job = jobManager.getActiveJob(player);
        Block block = event.getBlock();

        if (block.getType().toString().contains("ORE") || block.getType() == Material.STONE) {
            if (!"miner".equals(job)) {
                event.setCancelled(true);
                player.sendMessage("§c당신의 직업은 광물을 채굴할 수 없습니다.");
            }
        } else if (block.getType().name().contains("LOG") || block.getType().name().contains("WOOD")) {
            if (!"carpenter".equals(job)) {
                event.setCancelled(true);
                player.sendMessage("§c목수만 나무를 벌목할 수 있습니다.");
            }
        }
    }

    // 사냥/공격 제한 (동물 및 몹)
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        String job = jobManager.getActiveJob(player);

        if (!"hunter".equals(job) &&
                (event.getEntity().getType().isAlive() || event.getEntity().getType().isMonster())) {
            event.setCancelled(true);
            player.sendMessage("§c사냥꾼만 몹과 동물을 공격할 수 있습니다.");
        }
    }

    // 낚시 제한
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        String job = jobManager.getActiveJob(player);

        if (!"fisher".equals(job)) {
            event.setCancelled(true);
            player.sendMessage("§c어부만 낚시를 할 수 있습니다.");
        }
    }

    // 농사 행동 제한 (파종, 수확)
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String job = jobManager.getActiveJob(player);

        // 농부만 씨앗 파종 & 작물 수확
        Block block = event.getClickedBlock();
        if (block != null) {
            // 파종
            if (event.getItem() != null &&
                    event.getItem().getType().name().endsWith("_SEEDS") &&
                    !"farmer".equals(job)) {
                event.setCancelled(true);
                player.sendMessage("§c농부만 작물을 심을 수 있습니다.");
            }
            // 수확
            if ((block.getType().name().endsWith("_CROP") || block.getType().name().endsWith("_PLANT")) &&
                    !"farmer".equals(job) && event.getAction().toString().contains("RIGHT_CLICK")) {
                event.setCancelled(true);
                player.sendMessage("§c농부만 작물을 수확할 수 있습니다.");
            }
        }
    }

    // 조합 제한
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String job = jobManager.getActiveJob(player);

        ItemStack result = event.getRecipe().getResult();
        if (result.getType().toString().toLowerCase().contains("potion") && !"alchemist".equals(job)) {
            event.setCancelled(true);
            player.sendMessage("§c연금술사만 포션을 조합할 수 있습니다.");
        }
        if (result.getType().toString().toLowerCase().contains("bread") && !"chef".equals(job)) {
            event.setCancelled(true);
            player.sendMessage("§c요리사만 음식을 조리할 수 있습니다.");
        }
        // 대장장이 관련 추가
        if ((result.getType().toString().contains("SWORD") || result.getType().toString().contains("ARMOR")) &&
                !"blacksmith".equals(job)) {
            event.setCancelled(true);
            player.sendMessage("§c대장장이만 무기와 방어구를 제작 및 강화할 수 있습니다.");
        }
    }

    // TODO: 상점, 퀘스트, 스킬, 직업조합, 등급, 특성, 전용 아이템 등 추가 이벤트 구현 (GUI, 마우스 클릭, 서버 경제 반영)
}
