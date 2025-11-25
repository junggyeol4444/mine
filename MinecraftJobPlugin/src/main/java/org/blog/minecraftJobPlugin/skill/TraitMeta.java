package com.yourname.jobplugin.skill;

import java.util.Map;

public class TraitMeta {
    public final String name;
    public final String effect;

    public TraitMeta(String name, String effect) {
        this.name = name;
        this.effect = effect;
    }

    public static TraitMeta fromYaml(Map<?,?> map) {
        return new TraitMeta(
                (String)map.getOrDefault("name", ""),
                (String)map.getOrDefault("effect", "")
        );
    }
}