package org.blog.minecraftJobPlugin.quest;

import java.util.Map;

public class QuestMeta {
    public final String id;
    public final String name;
    public final int reward;
    public final String type;

    public QuestMeta(String id, String name, int reward, String type) {
        this.id = id;
        this.name = name;
        this.reward = reward;
        this.type = type;
    }

    public static QuestMeta fromYaml(Map<?,?> map) {
        if (map == null) return new QuestMeta("", "", 0, "daily");
        Object idO = map.get("id");
        Object nameO = map.get("name");
        Object rewardO = map.get("reward");
        Object typeO = map.get("type");
        String id = idO == null ? "" : String.valueOf(idO);
        String name = nameO == null ? "" : String.valueOf(nameO);
        int reward = 0;
        if (rewardO instanceof Number) reward = ((Number)rewardO).intValue();
        else if (rewardO != null) try { reward = Integer.parseInt(String.valueOf(rewardO)); } catch (Exception ignored){}
        String type = typeO == null ? "daily" : String.valueOf(typeO);
        return new QuestMeta(id, name, reward, type);
    }
}