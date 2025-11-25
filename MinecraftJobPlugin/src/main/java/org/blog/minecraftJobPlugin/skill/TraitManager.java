package com.yourname.jobplugin.skill;

import com.yourname.jobplugin.JobPlugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

public class TraitManager {

    private final JobPlugin plugin;
    // [플레이어] → 직업명 → 선택 특성 Set
    private final Map<UUID, Map<String, Set<String>>> traitChoices = new HashMap<>();
    // [플레이어] → 특성 포인트 Map
    private final Map<UUID, Map<String, Integer>> traitPoints = new HashMap<>();

    public TraitManager(JobPlugin plugin) {
        this.plugin = plugin;
        loadTraitData();
    }

    // 특성 목록 조회
    public List<TraitMeta> getTraits(String job) {
        List<TraitMeta> traits = new ArrayList<>();
        YamlConfiguration yaml = plugin.getConfig("traits");
        List<?> traitList = yaml.getList("traits." + job, new ArrayList<>());
        for (Object o : traitList) {
            if (o instanceof Map) traits.add(TraitMeta.fromYaml((Map<?,?>)o));
        }
        return traits;
    }

    // 특성 선택시 포인트 사용
    public boolean selectTrait(Player player, String job, String traitName) {
        int pt = getPoints(player, job);
        if (pt < 1) return false;
        traitChoices.computeIfAbsent(player.getUniqueId(), k->new HashMap<>())
                .computeIfAbsent(job, k->new HashSet<>()).add(traitName);
        traitPoints.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).put(job, pt-1);
        player.sendMessage("§b[" + job + "] 특성 [" + traitName + "] 선택!");
        return true;
    }

    public int getPoints(Player player, String job) {
        return traitPoints.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).getOrDefault(job, 0);
    }
    public void addPoint(Player player, String job, int pt) {
        int now = getPoints(player, job);
        traitPoints.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).put(job, now+pt);
    }

    public void saveAll() {
        // TODO: traitChoices, traitPoints 저장 구현
    }

    private void loadTraitData() {
        // TODO: traitChoices, traitPoints 파일/DB 로딩
    }
}