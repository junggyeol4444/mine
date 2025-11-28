package org.blog.minecraftJobPlugin.skill;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * SkillManager — 스킬 경험치/레벨 관리 + 간단한 효과 구현
 */
public class SkillManager {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final PluginDataUtil dataUtil;

    private final Map<UUID, Map<String, Integer>> skillLevels = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> skillExp = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public SkillManager(JobPlugin plugin, JobManager jm) {
        this.plugin = plugin;
        this.jobManager = jm;
        this.dataUtil = new PluginDataUtil(plugin);
        loadSkillData();
    }

    public boolean useSkill(Player player, String skillName) {
        UUID id = player.getUniqueId();
        int level = getSkillLevel(player, skillName);
        long now = System.currentTimeMillis();
        long cd = SkillMeta.loadFromConfig(skillName, plugin).cooldown * 1000L;
        cooldowns.computeIfAbsent(id, k->new HashMap<>());
        long last = cooldowns.get(id).getOrDefault(skillName, 0L);
        if (now - last < cd) {
            player.sendMessage("§c스킬 쿨타임 중입니다.");
            return false;
        }
        cooldowns.get(id).put(skillName, now);

        addSkillExp(player, skillName, 20);

        switch (skillName) {
            case "geoScan":
                player.sendMessage("§6[GeoScan] 주변 지형을 스캔했습니다.");
                break;
            case "fastLumber":
                PotionEffectType haste = PotionEffectType.getByName("FAST_DIGGING");
                if (haste == null) haste = PotionEffectType.getByName("DIG_SPEED");
                if (haste != null) {
                    player.addPotionEffect(new PotionEffect(haste, 20 * 10, 1));
                    player.sendMessage("§a[FastLumber] 벌목 속도 증가 (10초).");
                } else {
                    player.sendMessage("§a[FastLumber] (속도 효과를 적용할 수 없습니다: 서버 API 호환성 문제)");
                }
                break;
            case "veinDetect":
                player.sendMessage("§e[VeinDetect] 근처 광맥을 감지했습니다 (가상).");
                break;
            default:
                player.sendMessage("§d[" + skillName + "] 스킬을 사용했습니다.");
        }
        return true;
    }

    public int getSkillLevel(Player player, String skill) {
        return skillLevels.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).getOrDefault(skill, 1);
    }

    public int getSkillExp(Player player, String skill) {
        return skillExp.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).getOrDefault(skill, 0);
    }

    private void addSkillExp(Player player, String skill, int exp) {
        UUID id = player.getUniqueId();
        skillExp.computeIfAbsent(id, k->new HashMap<>());
        int before = getSkillExp(player, skill);
        int after = before + exp;
        skillExp.get(id).put(skill, after);

        int curLevel = getSkillLevel(player, skill);
        SkillMeta meta = SkillMeta.loadFromConfig(skill, plugin);
        if (after >= meta.getExpForLevel(curLevel + 1)) {
            skillLevels.computeIfAbsent(id, k->new HashMap<>()).put(skill, curLevel + 1);
            player.sendMessage("§d[" + skill + "] 레벨업! Lv." + (curLevel + 1));
        }
    }

    private void loadSkillData() {
        java.io.File playerDir = new java.io.File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) return;
        java.io.File[] files = playerDir.listFiles((d,n)->n.endsWith(".yml"));
        if (files == null) return;
        for (java.io.File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml",""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                if (cfg.isConfigurationSection("skills")) {
                    Map<String,Integer> lv = new HashMap<>();
                    Map<String,Integer> ex = new HashMap<>();
                    for (String s : cfg.getConfigurationSection("skills").getKeys(false)) {
                        lv.put(s, cfg.getInt("skills." + s + ".level", 1));
                        ex.put(s, cfg.getInt("skills." + s + ".exp", 0));
                    }
                    if (!lv.isEmpty()) skillLevels.put(uuid, lv);
                    if (!ex.isEmpty()) skillExp.put(uuid, ex);
                }
            } catch (Exception ignored) {}
        }
    }

    private void savePlayerSkillsSync(UUID uuid) {
        YamlConfiguration cfg = dataUtil.loadPlayerConfig(uuid);
        Map<String,Integer> lv = skillLevels.getOrDefault(uuid, Collections.emptyMap());
        Map<String,Integer> ex = skillExp.getOrDefault(uuid, Collections.emptyMap());
        for (Map.Entry<String,Integer> e : lv.entrySet()) cfg.set("skills." + e.getKey() + ".level", e.getValue());
        for (Map.Entry<String,Integer> e : ex.entrySet()) cfg.set("skills." + e.getKey() + ".exp", e.getValue());
        dataUtil.savePlayerConfigSync(uuid, cfg);
    }

    public void saveAllAsync() {
        Set<UUID> all = new HashSet<>();
        all.addAll(skillLevels.keySet());
        all.addAll(skillExp.keySet());
        for (UUID u : all) {
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(u);
            Map<String,Integer> lv = skillLevels.getOrDefault(u, Collections.emptyMap());
            Map<String,Integer> ex = skillExp.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String,Integer> e : lv.entrySet()) cfg.set("skills." + e.getKey() + ".level", e.getValue());
            for (Map.Entry<String,Integer> e : ex.entrySet()) cfg.set("skills." + e.getKey() + ".exp", e.getValue());
            dataUtil.savePlayerConfigAsync(u, cfg);
        }
    }

    public void saveAll() {
        Set<UUID> all = new HashSet<>();
        all.addAll(skillLevels.keySet());
        all.addAll(skillExp.keySet());
        for (UUID u : all) savePlayerSkillsSync(u);
    }

    public SkillMeta getSkillMeta(String skill) {
        return SkillMeta.loadFromConfig(skill, plugin);
    }
}