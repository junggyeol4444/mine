package org.blog.minecraftJobPlugin.job;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobMeta {
    public final String key;
    public final String display;
    public final String description;
    public final List<String> skills;

    // 시작 아이템 메타
    public final List<StartingItem> startingItems;

    public JobMeta(String key, ConfigurationSection section) {
        this.key = key;
        this.display = section.getString("display", key);
        this.description = section.getString("description", "");
        this.skills = section.getStringList("skills");
        this.startingItems = new ArrayList<>();

        if (section.isList("startingItems")) {
            List<?> nodes = section.getList("startingItems");
            for (Object o : nodes) {
                if (o instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) o;
                    String matName = String.valueOf(m.getOrDefault("material", "STONE"));
                    Material mat = Material.matchMaterial(matName);
                    if (mat == null) mat = Material.STONE;
                    int amount = ((Number) m.getOrDefault("amount", 1)).intValue();
                    String name = m.containsKey("name") ? String.valueOf(m.get("name")) : null;
                    Integer slot = m.containsKey("slot") ? ((Number)m.get("slot")).intValue() : null;
                    @SuppressWarnings("unchecked")
                    Map<String,Object> ench = (m.containsKey("enchants") && m.get("enchants") instanceof Map) ? (Map<String,Object>)m.get("enchants") : null;
                    startingItems.add(new StartingItem(mat, amount, name, slot, ench));
                }
            }
        }
    }

    public static class StartingItem {
        public final Material material;
        public final int amount;
        public final String displayName;
        public final Integer slot;
        public final Map<String, Object> enchants;

        public StartingItem(Material material, int amount, String displayName, Integer slot, Map<String, Object> enchants) {
            this.material = material;
            this.amount = amount;
            this.displayName = displayName;
            this.slot = slot;
            this.enchants = enchants;
        }
    }
}