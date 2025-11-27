package org.blog.minecraftJobPlugin.job;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * JobManager — YAML 기반 직업 관리 (수정: addJob 시 장비 지급)
 */
public class JobManager {

    private final JobPlugin plugin;
    private final PluginDataUtil dataUtil;

    private final Map<UUID, Set<String>> playerJobs = new HashMap<>();
    private final Map<UUID, String> activeJob = new HashMap<>();
    private final Map<String, JobMeta> jobMetas = new LinkedHashMap<>();

    public JobManager(JobPlugin plugin) {
        this.plugin = plugin;
        this.dataUtil = new PluginDataUtil(plugin);
        ensureDataFolders();
        loadJobMeta();
        loadPlayerJobs();
    }

    private void ensureDataFolders() {
        File d = new File(plugin.getDataFolder(), "data/player");
        if (!d.exists()) d.mkdirs();
    }

    private void loadJobMeta() {
        YamlConfiguration y = dataUtil.loadGlobal("jobs");
        if (y == null || !y.isConfigurationSection("jobs")) return;
        ConfigurationSection sec = y.getConfigurationSection("jobs");
        for (String key : sec.getKeys(false)) {
            ConfigurationSection jobSec = sec.getConfigurationSection(key);
            jobMetas.put(key, new JobMeta(key, jobSec));
        }
    }

    private void loadPlayerJobs() {
        File playerDir = new File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) return;
        File[] files = playerDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                List<String> jobs = cfg.getStringList("jobs");
                if (!jobs.isEmpty()) playerJobs.put(uuid, new HashSet<>(jobs));
                String active = cfg.getString("activeJob", null);
                if (active != null && !active.isBlank()) activeJob.put(uuid, active);
            } catch (Exception ignored) {}
        }
    }

    public Set<String> getPlayerJobs(Player player) {
        return Collections.unmodifiableSet(playerJobs.getOrDefault(player.getUniqueId(), Collections.emptySet()));
    }

    public String getActiveJob(Player player) {
        return activeJob.get(player.getUniqueId());
    }

    public JobMeta getJobMeta(String name) {
        return jobMetas.get(name);
    }

    public List<JobMeta> getAllJobs() {
        return new ArrayList<>(jobMetas.values());
    }

    public boolean hasJob(Player player, String jobName) {
        return getPlayerJobs(player).contains(jobName);
    }

    public void addJob(Player player, String jobName) {
        UUID id = player.getUniqueId();
        synchronized (playerJobs) {
            playerJobs.computeIfAbsent(id, k -> new HashSet<>()).add(jobName);
        }
        synchronized (activeJob) {
            activeJob.putIfAbsent(id, jobName);
        }
        // 지급: 장비 매니저에 위임
        try {
            plugin.getEquipmentManager().giveStartingItems(player, jobName);
        } catch (Exception ex) {
            plugin.getLogger().warning("시작 장비 지급 중 오류: " + ex.getMessage());
        }
    }

    public void setActiveJob(Player player, String jobName) {
        UUID id = player.getUniqueId();
        if (!hasJob(player, jobName)) return;
        synchronized (activeJob) {
            activeJob.put(id, jobName);
        }
    }

    public String rollRandomJob(Player player) {
        Set<String> current = getPlayerJobs(player);
        List<String> candidates = jobMetas.keySet().stream().filter(k -> !current.contains(k)).collect(Collectors.toList());
        if (candidates.isEmpty()) return null;
        String selected = candidates.get(new Random().nextInt(candidates.size()));
        addJob(player, selected);
        return selected;
    }

    public void saveAll() {
        // 보존: 모든 플레이어 jobs/active 저장 (구현은 기존 코드와 동일하게 프로젝트에 맞게 추가하세요)
    }

    public void saveAllAsync() {
        // 보존: 비동기 저장 구현 (프로젝트 상황에 맞게 추가)
    }
}