package com.yourname.jobplugin;

import com.yourname.jobplugin.command.JobCommand;
import com.yourname.jobplugin.command.ShopCommand;
import com.yourname.jobplugin.listener.JobEventListener;
import com.yourname.jobplugin.util.ConfigUtil;
import com.yourname.jobplugin.job.JobManager;
import com.yourname.jobplugin.econ.EconomyManager;
import com.yourname.jobplugin.quest.QuestManager;
import com.yourname.jobplugin.skill.SkillManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

public class JobPlugin extends JavaPlugin {

    private static JobPlugin instance;
    private JobManager jobManager;
    private EconomyManager economyManager;
    private QuestManager questManager;
    private SkillManager skillManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        ConfigUtil.loadAllConfigs(this);

        jobManager = new JobManager(this);
        economyManager = new EconomyManager(this, jobManager);
        questManager = new QuestManager(this, jobManager, economyManager);
        skillManager = new SkillManager(this, jobManager);

        // 명령어 등록
        getCommand("job").setExecutor(new JobCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));

        Bukkit.getPluginManager().registerEvents(new JobEventListener(this), this);

        getLogger().info("MinecraftJobPlugin 활성화 완료! (Paper 1.21.10)");
    }

    @Override
    public void onDisable() {
        getLogger().info("MinecraftJobPlugin 비활성화: 데이터 저장 및 시스템 자원 해제.");
        jobManager.saveAll();
        economyManager.saveAll();
        questManager.saveAll();
        skillManager.saveAll();
    }

    public static JobPlugin getInstance() { return instance; }
    public JobManager getJobManager() { return jobManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public QuestManager getQuestManager() { return questManager; }
    public SkillManager getSkillManager() { return skillManager; }
}