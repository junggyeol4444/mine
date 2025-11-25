package com.yourname.jobplugin.job;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

public class JobMeta {
    public final String id;
    public final String display;
    public final String description;
    public final List<String> skills;
    public final List<String> actions;
    public final String rarity;

    public JobMeta(String id, ConfigurationSection sec) {
        this.id = id;
        this.display = sec.getString("display");
        this.description = sec.getString("description");
        this.skills = sec.getStringList("skills");
        this.actions = sec.getStringList("actions");
        this.rarity = sec.getString("rarity", "common");
    }
}