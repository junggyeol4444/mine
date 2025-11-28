package org.blog.minecraftJobPlugin.listener;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.skill.SkillManager;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

/**
 * 스킬 버프 효과를 실제 게임플레이에 적용하는 리스너
 */
public class SkillBuffListener implements Listener {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final SkillManager skillManager;

    public SkillBuffListener(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.skillManager = plugin.getSkillManager();
    }

    /**
     * 효율적 조각 - 목재 드롭 증가
     */
    @EventHandler
    public void onTreeBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Material broken = e.getBlock().getType();
        
        // 목재인지 확인
        if (!broken.name().contains("LOG")) return;
        
        if (skillManager.hasActiveBuff(player, "efficientCarve")) {
            // 추가 드롭
            e.getBlock().getWorld().dropItemNaturally(
                e.getBlock().getLocation(), 
                new ItemStack(broken, 1)
            );
        }
    }

    /**
     * 풍작 - 수확량 증가
     */
    @EventHandler
    public void onHarvest(PlayerHarvestBlockEvent e) {
        Player player = e.getPlayer();
        
        if (skillManager.hasActiveBuff(player, "bumperCrop")) {
            // 수확량 20% 증가
            List<ItemStack> items = new ArrayList<>(e.getItemsHarvested());
            for (ItemStack item : items) {
                int bonus = (int)(item.getAmount() * 0.2);
                if (bonus > 0) {
                    e.getItemsHarvested().add(new ItemStack(item.getType(), bonus));
                }
            }
        }
    }

    /**
     * 희귀 입질 - 낚시 확률 증가
     */
    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = e.getPlayer();
        
        if (skillManager.hasActiveBuff(player, "rareCatch")) {
            // 희귀 아이템 확률 증가 (30% 확률로 추가 아이템)
            if (Math.random() < 0.3) {
                Material[] rareItems = {
                    Material.SALMON, Material.TROPICAL_FISH, 
                    Material.PUFFERFISH, Material.NAME_TAG,
                    Material.NAUTILUS_SHELL
                };
                Material rare = rareItems[new Random().nextInt(rareItems.length)];
                player.getWorld().dropItemNaturally(
                    player.getLocation(), 
                    new ItemStack(rare, 1)
                );
                player.sendMessage("§b[RareCatch] §f희귀 아이템 추가 획득!");
            }
        }
    }

    /**
     * 치명타 사격 - 화살 데미지 증가
     */
    @EventHandler
    public void onArrowLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Arrow)) return;
        if (!(e.getEntity().getShooter() instanceof Player)) return;
        
        Player player = (Player) e.getEntity().getShooter();
        
        if (skillManager.hasActiveBuff(player, "criticalShot")) {
            Arrow arrow = (Arrow) e.getEntity();
            arrow.setMetadata("critical_shot", new FixedMetadataValue(plugin, true));
            arrow.setCritical(true);
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Arrow)) return;
        Arrow arrow = (Arrow) e.getDamager();
        
        if (arrow.hasMetadata("critical_shot")) {
            e.setDamage(e.getDamage() * 1.5); // 데미지 50% 증가
            if (e.getEntity() instanceof Player) {
                ((Player) e.getEntity()).sendMessage("§c치명타!");
            }
        }
    }
}