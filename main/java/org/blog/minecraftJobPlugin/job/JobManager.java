package org.blog.minecraftJobPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.Job;
import org.blog.minecraftJobPlugin.util.ConfigUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobManager {

    private final JobPlugin plugin;

    // 직업 데이터 (직업 이름 -> Job 객체)
    private final Map<String, Job> jobs = new HashMap<>();

    // 플레이어 데이터 (UUID -> 보유 직업 목록)
    private final Map<UUID, List<String>> playerJobs = new ConcurrentHashMap<>();

    // 플레이어 활성 직업 (UUID -> 장착 중인 직업)
    private final Map<UUID, String> activeJobs = new ConcurrentHashMap<>();

    public JobManager(JobPlugin plugin) {
        this.plugin = plugin;
        loadJobs();
    }

    /**
     * 모든 직업 로드
     */
    private void loadJobs() {
        YamlConfiguration config = ConfigUtil.getConfig("jobs");

        if (config == null) {
            plugin.getLogger().warning("jobs.yml 파일을 찾을 수 없습니다!");
            return;
        }

        if (!config.contains("jobs")) {
            plugin.getLogger().warning("jobs.yml에 직업 정보가 없습니다!");
            return;
        }

        for (String jobName : config.getConfigurationSection("jobs").getKeys(false)) {
            try {
                Job job = Job.fromConfig(config, jobName);
                jobs.put(jobName, job);
                plugin.getLogger().info("직업 로드 완료: " + jobName);
            } catch (Exception e) {
                plugin.getLogger().severe("직업 로드 실패 (" + jobName + "): " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("총 " + jobs.size() + "개의 직업 로드 완료");
    }

    /**
     * 직업 이름으로 Job 객체 가져오기
     */
    public Job getJob(String jobName) {
        return jobs.get(jobName);
    }

    /**
     * 모든 직업 목록 반환
     */
    public List<Job> getAllJobs() {
        return new ArrayList<>(jobs.values());
    }

    /**
     * 플레이어가 특정 직업을 보유하고 있는지 확인
     */
    public boolean hasJob(Player player, String jobName) {
        UUID uuid = player.getUniqueId();
        if (!playerJobs.containsKey(uuid)) {
            return false;
        }
        return playerJobs.get(uuid).contains(jobName);
    }

    /**
     * 플레이어가 보유한 모든 직업 목록 반환
     */
    public List<String> getPlayerJobs(Player player) {
        UUID uuid = player.getUniqueId();
        return new ArrayList<>(playerJobs.getOrDefault(uuid, new ArrayList<>()));
    }

    /**
     * 플레이어에게 직업 추가
     */
    public void addJob(Player player, String jobName) {
        UUID uuid = player.getUniqueId();

        // 이미 보유한 직업이면 추가하지 않음
        if (hasJob(player, jobName)) {
            return;
        }

        // 직업이 존재하는지 확인
        if (!jobs.containsKey(jobName)) {
            plugin.getLogger().warning("존재하지 않는 직업: " + jobName);
            return;
        }

        // 플레이어의 직업 목록 가져오기
        List<String> jobList = playerJobs.computeIfAbsent(uuid, k -> new ArrayList<>());
        jobList.add(jobName);

        // 첫 직업이면 자동으로 장착
        if (jobList.size() == 1) {
            setActiveJob(player, jobName);
        }

        // 조합 체크
        if (plugin.getComboManager() != null) {
            plugin.getComboManager().checkCombos(player);
        }

        // 데이터 저장
        savePlayerData(player);

        plugin.getLogger().info(player.getName() + "님이 직업 획득: " + jobName);
    }

    /**
     * 플레이어의 활성 직업 설정
     */
    public void setActiveJob(Player player, String jobName) {
        UUID uuid = player.getUniqueId();

        // null이면 직업 해제
        if (jobName == null) {
            activeJobs.remove(uuid);
            savePlayerData(player);
            return;
        }

        // 보유하지 않은 직업은 장착 불가
        if (!hasJob(player, jobName)) {
            plugin.getLogger().warning(player.getName() + "님은 " + jobName + " 직업을 보유하고 있지 않습니다.");
            return;
        }

        activeJobs.put(uuid, jobName);
        savePlayerData(player);

        plugin.getLogger().info(player.getName() + "님이 직업 장착: " + jobName);
    }

    /**
     * 플레이어의 활성 직업 반환
     */
    public String getActiveJob(Player player) {
        return activeJobs.get(player.getUniqueId());
    }

    /**
     * 플레이어의 직업 제거
     */
    public void removeJob(Player player, String jobName) {
        UUID uuid = player.getUniqueId();

        if (!hasJob(player, jobName)) {
            return;
        }

        List<String> jobList = playerJobs.get(uuid);
        jobList.remove(jobName);

        // 삭제한 직업이 활성 직업이었다면 해제
        if (jobName.equals(activeJobs.get(uuid))) {
            activeJobs.remove(uuid);

            // 남은 직업이 있으면 첫 번째 직업을 자동 장착
            if (!jobList.isEmpty()) {
                setActiveJob(player, jobList.get(0));
            }
        }

        savePlayerData(player);

        plugin.getLogger().info(player.getName() + "님의 직업 제거: " + jobName);
    }

    /**
     * 플레이어 데이터 저장
     */
    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(plugin.getDataFolder(), "data/player/" + uuid + ".yml");

        if (!playerFile.getParentFile().exists()) {
            playerFile.getParentFile().mkdirs();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        // 보유 직업 저장
        List<String> jobList = playerJobs.getOrDefault(uuid, new ArrayList<>());
        config.set("jobs", jobList);

        // 활성 직업 저장
        String activeJob = activeJobs.get(uuid);
        config.set("active_job", activeJob);

        try {
            config.save(playerFile);

            if (plugin.getConfig().getBoolean("debug.log_save_operations", false)) {
                plugin.getLogger().info(player.getName() + " 직업 데이터 저장 완료");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("플레이어 직업 데이터 저장 실패 (" + player.getName() + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 플레이어 데이터 로드
     */
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(plugin.getDataFolder(), "data/player/" + uuid + ".yml");

        if (!playerFile.exists()) {
            // 신규 플레이어
            playerJobs.put(uuid, new ArrayList<>());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        // 보유 직업 로드
        List<String> jobList = config.getStringList("jobs");
        if (!jobList.isEmpty()) {
            playerJobs.put(uuid, new ArrayList<>(jobList));
        } else {
            playerJobs.put(uuid, new ArrayList<>());
        }

        // 활성 직업 로드
        String activeJob = config.getString("active_job");
        if (activeJob != null && !activeJob.isEmpty() && jobList.contains(activeJob)) {
            activeJobs.put(uuid, activeJob);
        }

        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info(player.getName() + " 직업 데이터 로드 완료: " + jobList);
        }
    }

    /**
     * 모든 온라인 플레이어 데이터 저장
     */
    public void saveAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }

        if (plugin.getConfig().getBoolean("debug.log_save_operations", false)) {
            plugin.getLogger().info("모든 플레이어의 직업 데이터 저장 완료");
        }
    }

    /**
     * 플레이어 데이터 언로드 (메모리에서 제거)
     */
    public void unloadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        playerJobs.remove(uuid);
        activeJobs.remove(uuid);
    }

    /**
     * 직업 정보 새로고침 (config 리로드)
     */
    public void reload() {
        jobs.clear();
        loadJobs();
        plugin.getLogger().info("직업 데이터 리로드 완료");
    }

    /**
     * 플레이어가 보유할 수 있는 최대 직업 개수 확인
     */
    public int getMaxJobsPerPlayer() {
        return plugin.getConfig().getInt("job.max_jobs_per_player", -1);
    }

    /**
     * 플레이어가 더 많은 직업을 획득할 수 있는지 확인
     */
    public boolean canAcquireMoreJobs(Player player) {
        int maxJobs = getMaxJobsPerPlayer();
        if (maxJobs == -1) {
            return true; // 무제한
        }

        int currentJobs = getPlayerJobs(player).size();
        return currentJobs < maxJobs;
    }

    /**
     * 직업 포기 가능 여부 확인
     */
    public boolean canAbandonJob() {
        return plugin.getConfig().getBoolean("job.can_abandon_job", false);
    }

    /**
     * 디버그 정보 출력
     */
    public void printDebugInfo() {
        plugin.getLogger().info("====== JobManager 디버그 정보 ======");
        plugin.getLogger().info("로드된 직업 수: " + jobs.size());
        plugin.getLogger().info("직업 목록: " + jobs.keySet());
        plugin.getLogger().info("메모리에 로드된 플레이어 수: " + playerJobs.size());
        plugin.getLogger().info("활성 직업을 가진 플레이어 수: " + activeJobs.size());
        plugin.getLogger().info("===================================");
    }
}