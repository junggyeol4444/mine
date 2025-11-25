package com.yourname.jobplugin.gui;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.skill.SkillManager;
import com.yourname.jobplugin.skill.TraitManager;
import com.yourname.jobplugin.skill.TraitMeta;
import com.yourname.jobplugin.job.JobManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SkillTreeGUI {

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

        Inventory inv = Bukkit.createInventory(player, 27, "스킬 트리 / 특성");

        // 스킬 레벨/경험치 바 & 효과
        List<String> jobSkills = plugin.getJobManager().getJobMeta(job).skills;
        for (int i = 0; i < jobSkills.size(); i++) {
            String skill = jobSkills.get(i);
            ItemStack icon = new ItemStack(Material.NETHER_STAR);
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName("§a" + skill + " Lv." + sm.getSkillLevel(player, skill));
            im.setLore(List.of(
                    "§7효과: " + sm.getSkillMeta(skill).levelEffects.getOrDefault(sm.getSkillLevel(player, skill), ""),
                    "§f경험치: " + sm.getSkillExp(player, skill),
                    "§e쿨타임: " + sm.getSkillMeta(skill).getCooldown(sm.getSkillLevel(player, skill)) + "초"
            ));
            icon.setItemMeta(im);
            inv.setItem(i, icon);
        }

        // 특성 트리
        List<TraitMeta> traits = tm.getTraits(job);
        for (int i = 0; i < traits.size(); i++) {
            TraitMeta trait = traits.get(i);
            ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName("§b" + trait.name);
            im.setLore(List.of(
                    "§f효과: " + trait.effect,
                    "§6상태: " + (tm.getPoints(player, job) > 0 ? "선택 가능" : "포인트 필요")
            ));
            icon.setItemMeta(im);
            inv.setItem(9 + i, icon);
        }
        player.openInventory(inv);

        // 특성 선택 이벤트는 별도 리스너에서 구현!
    }
}