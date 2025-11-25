package com.yourname.jobplugin.job;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.util.ConfigUtil;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

public class JobManager {

    private JobPlugin plugin;

    // [플레이어 UUID] → 보유 직업(직업명 Set)
    private final Map<UUID, Set<String>> playerJobs = new HashMap<>();
    // [플레이어 UUID] → 현재 장착 직업
    private final Map<UUID, String> activeJob = new HashMap<>();

    // 직업 목록
    private final Map<String, JobMeta> jobMetas = new HashMap<>();

    public JobManager(JobPlugin plugin) {
        this.plugin = plugin;
        loadJobMeta();
        loadPlayerJobs();
    }

    // 직업 설정 YAML → JobMeta 객체 변환
    private void loadJobMeta() {
        YamlConfiguration jobsYaml = ConfigUtil.getConfig("jobs");
        for (String key : jobsYaml.getConfigurationSection("jobs").getKeys(false)) {
            jobMetas.put(key, new JobMeta(key, jobsYaml.getConfigurationSection("jobs." + key)));
        }
    }

    // 플레이어별 직업 데이터 로드(저장소 연동, 예시로 yaml/local 추상화)
    private void loadPlayerJobs() {
        // TODO: 실제 서버 배포시 DB or 파일 연동
    }

    public Set<String> getPlayerJobs(Player player) {
        return playerJobs.getOrDefault(player.getUniqueId(), new HashSet<>());
    }

    public String getActiveJob(Player player) {
        return activeJob.getOrDefault(player.getUniqueId(), null);
    }

    public JobMeta getJobMeta(String name) {
        return jobMetas.get(name);
    }

    // 직업 부여
    public void addJob(Player player, String jobName) {
        playerJobs.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(jobName);
        // 최초 부여시 자동 장착
        if (!activeJob.containsKey(player.getUniqueId())) activeJob.put(player.getUniqueId(), jobName);
    }

    // 직업 선택
    public void setActiveJob(Player player, String jobName) {
        if (getPlayerJobs(player).contains(jobName)) {
            activeJob.put(player.getUniqueId(), jobName);
        }
    }

    public boolean hasJob(Player player, String jobName) {
        return getPlayerJobs(player).contains(jobName);
    }

    // 직업 삭제
    public void removeJob(Player player, String jobName) {
        Set<String> jobs = getPlayerJobs(player);
        jobs.remove(jobName);
        if (getActiveJob(player).equals(jobName)) {
            activeJob.remove(player.getUniqueId());
        }
    }

    // 저장 (플러그인 종료, 서버 전체 세이브 등)
    public void saveAll() {
        // TODO: 파일/DB에 playerJobs, activeJob 저장 구현
    }

    // 랜덤 직업 뽑기 -> 이미 가진 직업 제외 후 확률 반영
    public String rollRandomJob(Player player) {
        Set<String> current = getPlayerJobs(player);
        List<String> candidates = new ArrayList<>();
        for (String name : jobMetas.keySet())
            if (!current.contains(name)) candidates.add(name);

        if (candidates.isEmpty()) return null;

        // 희귀도 기반 확률 (직업 yaml 내 rarity 속성 활용 가능)
        int idx = new Random().nextInt(candidates.size());
        String selected = candidates.get(idx);

        addJob(player, selected);
        return selected;
    }

    // 모든 직업 목록 (이름/설명 포함)
    public List<JobMeta> getAllJobs() {
        return new ArrayList<>(jobMetas.values());
    }
}