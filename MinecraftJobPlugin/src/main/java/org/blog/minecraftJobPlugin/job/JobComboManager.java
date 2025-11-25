package com.yourname.jobplugin.job;

import com.yourname.jobplugin.JobPlugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class JobComboManager {
    private final JobPlugin plugin;
    // [플레이어 UUID] -> 활성화된 조합 Set
    private final Map<UUID, Set<String>> unlockedCombos = new HashMap<>();

    public JobComboManager(JobPlugin plugin) {
        this.plugin = plugin;
        loadCombos();
    }

    // 가능한 모든 조합 목록
    public List<ComboMeta> getAllCombos() {
        YamlConfiguration yaml = plugin.getConfig("combos");
        List<ComboMeta> combos = new ArrayList<>();
        for (String comboKey : yaml.getConfigurationSection("combos").getKeys(false)) {
            combos.add(ComboMeta.fromYaml(comboKey, yaml.getConfigurationSection("combos." + comboKey)));
        }
        return combos;
    }

    // 조합 해금
    public boolean unlockCombo(Player player, String comboKey) {
        unlockedCombos.computeIfAbsent(player.getUniqueId(), k->new HashSet<>()).add(comboKey);
        player.sendMessage("§d직업 조합 [" + comboKey + "] 콘텐츠/시너지 해금!");
        // TODO: 신규 제작/능력 해금
        return true;
    }

    public boolean hasCombo(Player player, String comboKey) {
        return unlockedCombos.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(comboKey);
    }

    public void saveAll() {
        // TODO: unlockedCombos 저장 구현
    }

    private void loadCombos() {
        // TODO: unlockedCombos 파일/DB 로딩
    }
}