package org.blog.minecraftJobPlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.blog.minecraftJobPlugin.econ.EconomyManager;
import org.blog.minecraftJobPlugin.listeners.*;
import org.blog.minecraftJobPlugin.manager.JobManager;
import org.blog.minecraftJobPlugin.skill.SkillManager;
import org.blog.minecraftJobPlugin.quest.QuestManager;
import org.blog.minecraftJobPlugin.equipment.EquipmentManager;
import org.blog.minecraftJobPlugin.job.JobGradeManager;
import org.blog.minecraftJobPlugin.job.JobComboManager;
import org.blog.minecraftJobPlugin.skill.TraitManager;
import org.blog.minecraftJobPlugin.util.ConfigUtil;

public class JobPlugin extends JavaPlugin {

    private static JobPlugin instance;

    private JobManager jobManager;
    private SkillManager skillManager;
    private QuestManager questManager;
    private EconomyManager economyManager;
    private EquipmentManager equipmentManager;
    private JobGradeManager gradeManager;
    private JobComboManager comboManager;
    private TraitManager traitManager;

    private Object vaultEconomy = null;  // Economy → Object로 변경
    private boolean useVault = false;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // 1. Config 로드
            saveDefaultConfig();
            ConfigUtil.loadAllConfigs(this);
            getLogger().info("설정 파일 로드 완료");

            // 2. Vault 초기화
            setupVault();

            // 3. 매니저 초기화 - 순서 중요!
            this.jobManager = new JobManager(this);
            this.economyManager = new EconomyManager(this, jobManager, vaultEconomy);
            this.skillManager = new SkillManager(this, jobManager);
            this.questManager = new QuestManager(this, jobManager, economyManager);
            this.equipmentManager = new EquipmentManager(this);
            this.gradeManager = new JobGradeManager(this);
            this.comboManager = new JobComboManager(this);
            this.traitManager = new TraitManager(this);

            // 4. 리스너 등록
            registerListeners();

            // 5. 명령어 등록
            registerCommands();

            // 6. 자동 저장 스케줄러
            startAutoSaveScheduler();

            // 7. 시작 메시지
            if (getConfig().getBoolean("misc.show_startup_message", true)) {
                if (useVault) {
                    getLogger().info("MinecraftJobPlugin 활성화 완료! (Vault Economy 사용)");
                } else {
                    getLogger().info("MinecraftJobPlugin 활성화 완료! (내부 Economy 사용)");
                }
            }

        } catch (Exception e) {
            getLogger().severe("플러그인 초기화 실패: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Vault 초기화
     */
    private void setupVault() {
        boolean configUseVault = getConfig().getBoolean("vault.use_vault_economy", true);
        boolean warnIfNotFound = getConfig().getBoolean("vault.warn_if_not_found", false);

        if (!configUseVault) {
            getLogger().info("Config에서 Vault 사용 비활성화됨. 내부 Economy 사용.");
            return;
        }

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            if (warnIfNotFound) {
                getLogger().warning("Vault 플러그인을 찾을 수 없습니다. 내부 Economy 사용.");
            } else {
                getLogger().info("Vault 미설치. 내부 Economy 사용.");
            }
            return;
        }

        try {
            // Reflection을 사용하여 Vault 클래스 로드
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");

            RegisteredServiceProvider<?> rsp = getServer().getServicesManager()
                    .getRegistration(economyClass);

            if (rsp == null) {
                getLogger().warning("Vault는 설치되어 있으나 Economy 서비스가 등록되지 않았습니다.");
                getLogger().warning("Economy 플러그인(예: EssentialsX)이 필요합니다. 내부 Economy 사용.");
                return;
            }

            vaultEconomy = rsp.getProvider();

            if (vaultEconomy == null) {
                getLogger().warning("Vault Economy 서비스를 가져올 수 없습니다. 내부 Economy 사용.");
                return;
            }

            useVault = true;

            // getName() 메서드 호출 (Reflection)
            String providerName = "Unknown";
            try {
                providerName = (String) vaultEconomy.getClass().getMethod("getName").invoke(vaultEconomy);
            } catch (Exception ignored) {
            }

            getLogger().info("✓ Vault Economy 연동 완료! (제공자: " + providerName + ")");

        } catch (ClassNotFoundException e) {
            getLogger().info("Vault API를 찾을 수 없습니다. 내부 Economy 사용.");
            if (warnIfNotFound) {
                getLogger().warning("build.gradle에 Vault 의존성을 추가하세요.");
            }
        } catch (Exception e) {
            getLogger().warning("Vault 초기화 중 오류 발생: " + e.getMessage());
            getLogger().warning("내부 Economy 사용.");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JobGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);

        // 추가 리스너들
        getServer().getPluginManager().registerEvents(new org.blog.minecraftJobPlugin.listener.QuestTrackerListener(this), this);
        getServer().getPluginManager().registerEvents(new org.blog.minecraftJobPlugin.listener.SkillBuffListener(this), this);
        getServer().getPluginManager().registerEvents(new org.blog.minecraftJobPlugin.listener.SkillItemListener(this), this);
        getServer().getPluginManager().registerEvents(new org.blog.minecraftJobPlugin.listener.ComboCheckListener(this), this);
        getServer().getPluginManager().registerEvents(new org.blog.minecraftJobPlugin.listener.JobEventListener(this), this);

        getLogger().info("모든 이벤트 리스너 등록 완료");
    }

    private void registerCommands() {
        getCommand("job").setExecutor(new org.blog.minecraftJobPlugin.commands.JobCommand(this));
        getCommand("quest").setExecutor(new org.blog.minecraftJobPlugin.command.QuestCommand(this));
        getCommand("shop").setExecutor(new org.blog.minecraftJobPlugin.command.ShopCommand(this));
        getCommand("skill").setExecutor(new org.blog.minecraftJobPlugin.command.SkillCommand(this));
        getCommand("combo").setExecutor(new org.blog.minecraftJobPlugin.command.ComboCommand(this));
        getCommand("grade").setExecutor(new org.blog.minecraftJobPlugin.command.GradeCommand(this));
        getCommand("upgrade").setExecutor(new org.blog.minecraftJobPlugin.command.UpgradeCommand(this));
        getLogger().info("명령어 등록 완료");
    }

    private void startAutoSaveScheduler() {
        int saveIntervalSeconds = getConfig().getInt("economy.autosave_seconds", 300);
        long saveIntervalTicks = saveIntervalSeconds * 20L;

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (getConfig().getBoolean("performance.async_save", true)) {
                Bukkit.getScheduler().runTaskAsynchronously(this, this::saveAllData);
            } else {
                saveAllData();
            }
        }, saveIntervalTicks, saveIntervalTicks);

        getLogger().info("자동 저장 스케줄러 시작 (주기: " + saveIntervalSeconds + "초)");
    }

    private void saveAllData() {
        try {
            jobManager.saveAll();
            skillManager.saveAll();
            questManager.saveAll();
            economyManager.saveAll();
            equipmentManager.saveAll();
            gradeManager.saveAll();
            traitManager.saveAll();

            if (getConfig().getBoolean("performance.announce_autosave", true)) {
                getLogger().info("플레이어 데이터 자동 저장 완료");
            }
        } catch (Exception e) {
            getLogger().severe("자동 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("플러그인 종료 중... 데이터 저장 시작");

            // 모든 데이터 동기 저장
            if (jobManager != null) jobManager.saveAll();
            if (skillManager != null) skillManager.saveAll();
            if (questManager != null) questManager.saveAll();
            if (economyManager != null) economyManager.saveAll();
            if (equipmentManager != null) equipmentManager.saveAll();
            if (gradeManager != null) gradeManager.saveAll();
            if (traitManager != null) traitManager.saveAll();

            getLogger().info("모든 데이터 저장 완료. MinecraftJobPlugin 비활성화");
        } catch (Exception e) {
            getLogger().severe("플러그인 비활성화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== Getters ====================

    public static JobPlugin getInstance() {
        return instance;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public EquipmentManager getEquipmentManager() {
        return equipmentManager;
    }

    public JobGradeManager getGradeManager() {
        return gradeManager;
    }

    public JobComboManager getComboManager() {
        return comboManager;
    }

    public TraitManager getTraitManager() {
        return traitManager;
    }

    public Object getVaultEconomy() {  // Economy → Object로 변경
        return vaultEconomy;
    }

    public boolean isUsingVault() {
        return useVault;
    }
}