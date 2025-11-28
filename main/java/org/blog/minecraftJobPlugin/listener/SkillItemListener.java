package org.blog.minecraftJobPlugin.listener;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.job.JobMeta;
import org.blog.minecraftJobPlugin.skill.SkillManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 특수 아이템(스킬 아이템)으로 스킬 발동
 * - 스킬북(책)을 들고 우클릭하면 스킬 발동
 */
public class SkillItemListener implements Listener {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final SkillManager skillManager;
    private final NamespacedKey skillKey;

    public SkillItemListener(JobPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.skillManager = plugin.getSkillManager();
        this.skillKey = new NamespacedKey(plugin, "skill_type");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();

        // 우클릭만 처리
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 아이템이 없으면 무시
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(skillKey, PersistentDataType.STRING)) {
            return;
        }

        // 스킬 타입 확인
        String skillName = meta.getPersistentDataContainer().get(skillKey, PersistentDataType.STRING);
        if (skillName == null || skillName.isEmpty()) {
            return;
        }

        // 직업 확인
        String activeJob = jobManager.getActiveJob(player);
        if (activeJob == null) {
            player.sendMessage("§c직업을 먼저 장착해주세요.");
            return;
        }

        JobMeta jobMeta = jobManager.getJobMeta(activeJob);
        if (jobMeta == null || !jobMeta.skills.contains(skillName)) {
            player.sendMessage("§c현재 직업으로는 이 스킬을 사용할 수 없습니다.");
            return;
        }

        // 스킬 발동
        e.setCancelled(true); // 블록 설치 등 방지
        skillManager.useSkill(player, skillName);
    }

    /**
     * 스킬북 생성 유틸리티 (다른 곳에서 호출 가능)
     */
    public ItemStack createSkillBook(String skillName, String displayName, String description) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        meta.setDisplayName("§d§l" + displayName);
        meta.setLore(java.util.Arrays.asList(
                "§7" + description,
                "",
                "§e우클릭으로 스킬 발동"
        ));

        meta.getPersistentDataContainer().set(skillKey, PersistentDataType.STRING, skillName);
        book.setItemMeta(meta);

        return book;
    }
}