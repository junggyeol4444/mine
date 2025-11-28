package org.blog.minecraftJobPlugin.skill;

import java.util.Map;

public class TraitMeta {
    public final String name;
    public final String effect;

    public TraitMeta(String name, String effect) {
        this.name = name;
        this.effect = effect;
    }

    public static TraitMeta fromYaml(Map<?,?> map) {
        if (map == null) return new TraitMeta("", "");
        Object n = map.get("name");
        Object e = map.get("effect");
        return new TraitMeta(n == null ? "" : String.valueOf(n), e == null ? "" : String.valueOf(e));
    }
}