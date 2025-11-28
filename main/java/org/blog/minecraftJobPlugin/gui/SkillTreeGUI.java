// SkillTreeGUI.java에 추가
package org.blog.minecraftJobPlugin.gui;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.skill.SkillManager;
import org.blog.minecraftJobPlugin.skill.TraitManager;
import org.blog.minecraftJobPlugin.skill.TraitMeta;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SkillTreeGUI {

    private static final String TITLE = "스킬 트리 / 특성";

    private final JobPlugin plugin;
    private final Player player;

    public SkillTreeGUI(JobPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        String job = plugin.getJobManager().getActiveJob(player);
        if (job == null) {
            player.sendMessage("§c직업 장착 필요");
            return;
        }
        SkillManager sm = plugin.getSkillManager();
        TraitManager tm = plugin.getTraitManager();

        Inventory inv = Bukkit.createInventory(player, 36, TITLE);

        List<String> jobSkills = plugin.getJobManager().getJobMeta(job).skills;
        for (int i = 0; i < jobSkills.size() && i < 9; i++) {
            String skill = jobSkills.get(i);
            ItemStack icon = new ItemStack(Material.NETHER_STAR);
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName("§a" + skill + " Lv." + sm.getSkillLevel(player, skill));
            im.setLore(List.of(
                    "§7효과: " + sm.getSkillMeta(skill).levelEffects.getOrDefault(sm.getSkillLevel(player, skill), ""),
                    "§f경험치: " + sm.getSkillExp(player, skill),
                    "§e쿨타임: " + sm.getSkillMeta(skill).getCooldown(sm.getSkillLevel(player, skill)) + "초",
                    "",
                    "§e좌클릭: 스킬 사용",
                    "§e우클릭: 스킬북 받기"
            ));
            icon.setItemMeta(im);
            inv.setItem(i, icon);
        }

        List<TraitMeta> traits = tm.getTraits(job);
        for (int i = 0; i < traits.size() && i < 18; i++) {
            TraitMeta trait = traits.get(i);
            ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName("§b" + trait.name);
            im.setLore(List.of(
                    "§f효과: " + trait.effect,
                    "§6상태: " + (tm.getPoints(player, job) > 0 ? "선택 가능" : "포인트 필요"),
                    "",
                    "§e클릭: 특성 선택"
            ));
            icon.setItemMeta(im);
            inv.setItem(18 + i, icon);
        }
        player.openInventory(inv);
    }

    // 누락된 메서드 1: 인벤토리 타이틀 확인
    public static boolean isSkillTreeInventory(String title) {
        return TITLE.equals(title);
    }

    // 누락된 메서드 2: 슬롯에서 스킬명 가져오기
    public static String getSkillNameFromSlot(JobPlugin plugin, Player player, int slot) {
        String job = plugin.getJobManager().getActiveJob(player);
        if (job == null) return null;

        List<String> skills = plugin.getJobManager().getJobMeta(job).skills;
        if (slot >= 0 && slot < skills.size()) {
            return skills.get(slot);
        }
        return null;
    }

    // 누락된 메서드 3: 슬롯에서 특성 가져오기
    public static TraitMeta getTraitFromSlot(JobPlugin plugin, Player player, int slot) {
        String job = plugin.getJobManager().getActiveJob(player);
        if (job == null) return null;

        // 특성은 18번 슬롯부터 시작
        int traitIndex = slot - 18;

        List<TraitMeta> traits = plugin.getTraitManager().getTraits(job);
        if (traitIndex >= 0 && traitIndex < traits.size()) {
            return traits.get(traitIndex);
        }
        return null;
    }
}