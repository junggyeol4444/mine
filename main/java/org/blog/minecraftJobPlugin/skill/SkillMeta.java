package org.blog.minecraftJobPlugin.skill;

import org.blog.minecraftJobPlugin.util.ConfigUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class SkillMeta {
    public final String key;
    public final String display;
    public final int cooldown;
    public final Map<Integer, String> levelEffects;

    public SkillMeta(String key, String display, int cooldown, Map<Integer, String> levelEffects) {
        this.key = key;
        this.display = display;
        this.cooldown = cooldown;
        this.levelEffects = levelEffects != null ? levelEffects : new HashMap<>();
    }

    public int getExpForLevel(int level) {
        return 100 * level * level;
    }

    public int getCooldown(int level) {
        int reduce = 0;
        if (level == 2) reduce = 1;
        else if (level == 3) reduce = 2;
        else if (level >= 4) reduce = 4;
        return Math.max(1, cooldown - reduce);
    }

    public static SkillMeta loadFromConfig(String skill, JavaPlugin plugin) {
        YamlConfiguration yaml = ConfigUtil.getConfig(plugin, "skills");
        String display = skill;
        int cd = 10;
        Map<Integer,String> eff = new HashMap<>();
        if (yaml != null) {
            display = yaml.getString("skills." + skill + ".display", skill);
            cd = yaml.getInt("skills." + skill + ".cooldown", 10);
            String levelsPath = "skills." + skill + ".levels";
            if (yaml.isConfigurationSection(levelsPath)) {
                for (String k : yaml.getConfigurationSection(levelsPath).getKeys(false)) {
                    try {
                        int lv = Integer.parseInt(k);
                        eff.put(lv, yaml.getString(levelsPath + "." + k + ".effect", ""));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return new SkillMeta(skill, display, cd, eff);
    }
}