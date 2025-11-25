package com.yourname.jobplugin.quest;

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

    // yaml map → 객체 변환
    @SuppressWarnings("unchecked")
    public static QuestMeta fromYaml(Map<?,?> map) {
        return new QuestMeta(
                (String)map.getOrDefault("id", ""),
                (String)map.getOrDefault("name", ""),
                (int)map.getOrDefault("reward", 0),
                (String)map.getOrDefault("type", "daily")
        );
    }
}