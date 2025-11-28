package org.blog.minecraftJobPlugin;

import org.blog.minecraftJobPlugin.command.ComboCommand;
import org.blog.minecraftJobPlugin.command.GradeCommand;
import org.blog.minecraftJobPlugin.command.JobCommand;
import org.blog.minecraftJobPlugin.command.QuestCommand;
import org.blog.minecraftJobPlugin.command.ShopCommand;
import org.blog.minecraftJobPlugin.command.SkillCommand;
import org.blog.minecraftJobPlugin.command.UpgradeCommand;
import org.blog.minecraftJobPlugin.econ.EconomyManager;
import org.blog.minecraftJobPlugin.equipment.EquipmentManager;
import org.blog.minecraftJobPlugin.job.JobComboManager;
import org.blog.minecraftJobPlugin.job.JobGradeManager;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.listener.ComboCheckListener;
import org.blog.minecraftJobPlugin.listener.JobEventListener;
import org.blog.minecraftJobPlugin.listener.JobGuiListener;
import org.blog.minecraftJobPlugin.listener.QuestTrackerListener;
import org.blog.minecraftJobPlugin.listener.SkillBuffListener;
import org.blog.minecraftJobPlugin.listener.SkillItemListener;
import org.blog.minecraftJobPlugin.quest.QuestManager;
import org.blog.minecraftJobPlugin.skill.SkillManager;
import org.blog.minecraftJobPlugin.skill.TraitManager;
import org.blog.minecraftJobPlugin.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class JobPlugin extends JavaPlugin {

    private static JobPlugin instance;

    private JobManager jobManager;
    private EconomyManager economyManager;
    private QuestManager questManager;
    private SkillManager skillManager;
    private TraitManager traitManager;
    private JobComboManager comboManager;
    private JobGradeManager gradeManager;
    private EquipmentManager equipmentManager;

    // Vault reflection object (nullable)
    private Object vaultEcon;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        ConfigUtil.loadAllConfigs(this);

        setupVault();

        equipmentManager = new EquipmentManager(this);

        jobManager = new JobManager(this);
        economyManager = new EconomyManager(this, jobManager, vaultEcon);
        questManager = new QuestManager(this, jobManager, economyManager);
        skillManager = new SkillManager(this, jobManager);
        traitManager = new TraitManager(this);
        comboManager = new JobComboManager(this);
        gradeManager = new JobGradeManager(this);

        // 명령어 등록
        if (getCommand("job") != null) getCommand("job").setExecutor(new JobCommand(this));
        if (getCommand("shop") != null) getCommand("shop").setExecutor(new ShopCommand(this));
        if (getCommand("upgrade") != null) getCommand("upgrade").setExecutor(new UpgradeCommand(this));
        if (getCommand("skill") != null) getCommand("skill").setExecutor(new SkillCommand(this));
        if (getCommand("quest") != null) getCommand("quest").setExecutor(new QuestCommand(this));
        if (getCommand("combo") != null) getCommand("combo").setExecutor(new ComboCommand(this));
        if (getCommand("grade") != null) getCommand("grade").setExecutor(new GradeCommand(this));

        // 리스너 등록
        Bukkit.getPluginManager().registerEvents(new JobEventListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JobGuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SkillItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SkillBuffListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuestTrackerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ComboCheckListener(this), this);

        // 자동 저장
        int autosave = getConfig().getInt("economy.autosave_seconds", 300);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            getLogger().info("자동 저장(비동기) 실행...");
            jobManager.saveAllAsync();
            economyManager.saveAllAsync();
            questManager.saveAllAsync();
            skillManager.saveAllAsync();
            traitManager.saveAllAsync();
            comboManager.saveAllAsync();
            gradeManager.saveAllAsync();
        }, autosave * 20L, autosave * 20L);

        String vaultStatus = (vaultEcon != null) ? "사용" : "미사용";
        getLogger().info("MinecraftJobPlugin 활성화 완료! (Vault " + vaultStatus + ")");
    }

    @Override
    public void onDisable() {
        getLogger().info("플러그인 종료: 동기 저장 시작...");
        jobManager.saveAll();
        economyManager.saveAll();
        questManager.saveAll();
        skillManager.saveAll();
        traitManager.saveAll();
        comboManager.saveAll();
        gradeManager.saveAll();
        equipmentManager.saveAll();
        getLogger().info("저장 완료.");
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private void setupVault() {
        try {
            Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider rsp = getServer().getServicesManager().getRegistration((Class) econClass);
            if (rsp != null) {
                vaultEcon = rsp.getProvider();
                getLogger().info("Vault Economy 연결됨 (reflection).");
            } else {
                getLogger().info("Vault 미설치 또는 Economy 서비스 미존재. 내부 Economy 사용.");
            }
        } catch (ClassNotFoundException ex) {
            getLogger().info("Vault API 없음 — 내부 Economy 사용");
        } catch (Exception e) {
            getLogger().warning("Vault 초기화 중 오류(Reflection): " + e.getMessage());
        }
    }

    public static JobPlugin getInstance() { return instance; }
    public JobManager getJobManager() { return jobManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public QuestManager getQuestManager() { return questManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public TraitManager getTraitManager() { return traitManager; }
    public JobComboManager getComboManager() { return comboManager; }
    public JobGradeManager getGradeManager() { return gradeManager; }
    public EquipmentManager getEquipmentManager() { return equipmentManager; }
    public Object getVaultEconomy() { return vaultEcon; }
}