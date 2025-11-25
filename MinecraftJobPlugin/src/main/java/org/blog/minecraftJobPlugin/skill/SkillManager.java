package com.yourname.jobplugin.skill;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.job.JobManager;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SkillManager {

    private final JobPlugin plugin;
    private final JobManager jobManager;

    // [플레이어, 스킬명] 쿨타임 종료시각
    private final Map<UUID, Map<String, Long>> skillCooldowns = new HashMap<>();
    // [플레이어, 스킬명] 레벨
    private final Map<UUID, Map<String, Integer>> skillLevels = new HashMap<>();
    // [플레이어, 스킬명] 경험치
    private final Map<UUID, Map<String, Integer>> skillExp = new HashMap<>();

    public SkillManager(JobPlugin plugin, JobManager jobManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        loadSkillData();
    }

    // 스킬 발동 요청
    public boolean useSkill(Player player, String skillName) {
        int level = getSkillLevel(player, skillName);
        long now = System.currentTimeMillis();
        long cooldownEnd = getSkillCooldownEnd(player, skillName);

        SkillMeta skillMeta = getSkillMeta(skillName);
        if (cooldownEnd > now) {
            long left = (cooldownEnd-now)/1000;
            player.sendMessage("§c스킬 쿨타임 중입니다. (" + left + "초)");
            return false;
        }
        // 실제 발동 효과 (이벤트/버프 적용 등 구현! → SkillMeta 효과 해석 & 적용)
        player.sendMessage("§b[" + skillMeta.display + "] 스킬 Lv." + level + " 발동!");

        // 쿨타임 갱신
        setSkillCooldownEnd(player, skillName, now + skillMeta.getCooldown(level) * 1000);

        // 경험치 증가
        addSkillExp(player, skillName, 10);

        // GUI/액션바에 쿨타임 표시
        showSkillCooldown(player, skillName);
        return true;
    }

    // 쿨타임 표시 (액션바)
    public void showSkillCooldown(Player player, String skillName) {
        long now = System.currentTimeMillis();
        long end = getSkillCooldownEnd(player, skillName);
        int leftSec = (int)((end-now)/1000);
        if (leftSec > 0) {
            player.sendActionBar("§e스킬 [" + skillName + "] 쿨타임: §c" + leftSec + "초");
        }
    }

    // 쿨타임 조회/갱신
    public long getSkillCooldownEnd(Player player, String key) {
        return skillCooldowns
                .getOrDefault(player.getUniqueId(), new HashMap<>())
                .getOrDefault(key, 0L);
    }
    public void setSkillCooldownEnd(Player player, String key, long end) {
        skillCooldowns.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).put(key, end);
    }

    // 스킬 경험치/레벨
    public int getSkillExp(Player player, String skill) {
        return skillExp.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).getOrDefault(skill, 0);
    }
    public int getSkillLevel(Player player, String skill) {
        return skillLevels.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).getOrDefault(skill, 1);
    }
    private void addSkillExp(Player player, String skill, int exp) {
        int before = getSkillExp(player, skill);
        int after = before + exp;
        skillExp.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).put(skill, after);

        int curLevel = getSkillLevel(player, skill);
        SkillMeta meta = getSkillMeta(skill);

        if (after >= meta.getExpForLevel(curLevel+1)) {
            skillLevels.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).put(skill, curLevel+1);
            player.sendMessage("§d[" + skill + "] 스킬 레벨업! Lv." + (curLevel+1));
        }
    }

    // SkillMeta 조회
    public SkillMeta getSkillMeta(String skill) {
        return SkillMeta.loadFromConfig(skill, plugin);
    }

    // 전체 로딩/저장
    private void loadSkillData() {
        // TODO: 파일/DB에서 데이터 로딩
    }
    public void saveAll() {
        // TODO: 파일/DB에 skillCooldown, level, exp 등 저장
    }
}