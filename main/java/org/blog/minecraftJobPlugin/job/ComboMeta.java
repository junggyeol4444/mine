package com.yourname.jobplugin.job;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

public class ComboMeta {
    public final String key;
    public final String unlockEffect;
    public final List<String> requirements;

    public ComboMeta(String key, String unlockEffect, List<String> requirements) {
        this.key = key;
        this.unlockEffect = unlockEffect;
        this.requirements = requirements;
    }

    public static ComboMeta fromYaml(String key, ConfigurationSection sec) {
        return new ComboMeta(
                key,
                sec.getString("unlock"),
                sec.getStringList("requirements")
        );
    }
}