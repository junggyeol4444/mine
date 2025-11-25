package com.yourname.jobplugin.quest;

public class QuestProgress {
    public final String questId;
    public int progress;

    public QuestProgress(String questId, int progress) {
        this.questId = questId;
        this.progress = progress;
    }
}