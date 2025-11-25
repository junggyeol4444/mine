package com.yourname.jobplugin.skill;

import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public class SkillMeta {
    public final String key;
    public final String display;
    public final int baseCooldown;
    public final Map<Integer, String> levelEffects;

    public SkillMeta(String key, String display, int baseCooldown, Map<Integer, String> effectMap) {
        this.key = key;
        this.display = display;
        this.baseCooldown = baseCooldown;
        this.levelEffects = effectMap;
    }

    // 등급별 쿨타임 계산 (레벨, 특성, 등급 연계)
    public int getCooldown(int level) {
        int reduce = 0;
        if (level == 2) reduce = 1;
        if (level == 3) reduce = 2;
        if (level >= 4) reduce = 4;
        return Math.max(1, baseCooldown - reduce);
    }

    // 스킬 레벨별 요구 경험치 예시
    public int getExpForLevel(int level) {
        return 100 * level * level;
    }

    public static SkillMeta loadFromConfig(String skill, Plugin plugin) {
        YamlConfiguration yaml = (YamlConfiguration)plugin.getConfig("skills");
        String display = yaml.getString("skills." + skill + ".display", skill);
        int baseCd = yaml.getInt("skills." + skill + ".cooldown", 10);

        // 레벨별 효과는 생략/확장 가능
        return new SkillMeta(skill, display, baseCd, Map.of(
                1, yaml.getString("skills." + skill + ".levels.1.effect", ""),
                2, yaml.getString("skills." + skill + ".levels.2.effect", ""),
                3, yaml.getString("skills." + skill + ".levels.3.effect", ""),
                4, yaml.getString("skills." + skill + ".levels.4.effect", ""),
                5, yaml.getString("skills." + skill + ".levels.5.effect", "")
        ));
    }
}