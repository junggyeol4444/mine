package org.blog.minecraftJobPlugin.job;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * JobComboManager - 직업 조합 시스템
 * - 자동 조합 체크
 * - 조합 효과 적용
 */
public class JobComboManager {
    private final JobPlugin plugin;
    private final PluginDataUtil dataUtil;
    private final Map<UUID, Set<String>> unlockedCombos = new HashMap<>();

    public JobComboManager(JobPlugin plugin) {
        this.plugin = plugin;
        this.dataUtil = new PluginDataUtil(plugin);
        loadCombos();
    }

    public List<String> getAllCombos() {
        YamlConfiguration yaml = dataUtil.loadGlobal("combos");
        if (yaml == null || !yaml.isConfigurationSection("combos")) return Collections.emptyList();
        return new ArrayList<>(yaml.getConfigurationSection("combos").getKeys(false));
    }

    /**
     * 플레이어가 해금한 조합 목록 조회
     */
    public Set<String> getUnlockedCombos(Player player) {
        return Collections.unmodifiableSet(
                unlockedCombos.getOrDefault(player.getUniqueId(), Collections.emptySet())
        );
    }

    public boolean hasCombo(Player player, String comboKey) {
        return unlockedCombos.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(comboKey);
    }

    /**
     * 플레이어의 직업을 확인하여 해금 가능한 조합을 모두 체크
     */
    public void checkAndUnlockCombos(Player player) {
        YamlConfiguration combos = dataUtil.loadGlobal("combos");
        if (combos == null || !combos.isConfigurationSection("combos")) return;

        Set<String> playerJobs = plugin.getJobManager().getPlayerJobs(player);
        if (playerJobs.isEmpty()) return;

        for (String comboKey : combos.getConfigurationSection("combos").getKeys(false)) {
            // 이미 해금했으면 스킵
            if (hasCombo(player, comboKey)) continue;

            List<String> requirements = combos.getStringList("combos." + comboKey + ".requirements");
            if (requirements.isEmpty()) continue;

            // 모든 필요 직업을 보유했는지 확인
            boolean canUnlock = true;
            for (String req : requirements) {
                if (!playerJobs.contains(req)) {
                    canUnlock = false;
                    break;
                }
            }

            if (canUnlock) {
                unlockCombo(player, comboKey);
            }
        }
    }

    public boolean unlockCombo(Player player, String comboKey) {
        unlockedCombos.computeIfAbsent(player.getUniqueId(), k->new HashSet<>()).add(comboKey);
        savePlayerCombosAsync(player.getUniqueId());

        YamlConfiguration combos = dataUtil.loadGlobal("combos");
        String unlock = combos.getString("combos." + comboKey + ".unlock", comboKey);

        player.sendMessage("§a━━━━━━ 직업 조합 해금! ━━━━━━");
        player.sendMessage("§6§l" + comboKey);
        player.sendMessage("§f" + unlock);
        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━");

        // 조합 효과 적용
        applyComboEffect(player, comboKey);

        return true;
    }

    /**
     * 조합 효과 적용
     */
    private void applyComboEffect(Player player, String comboKey) {
        YamlConfiguration combos = dataUtil.loadGlobal("combos");
        String effect = combos.getString("combos." + comboKey + ".effect", "");

        // 효과 타입별 처리
        switch (comboKey) {
            case "miner_blacksmith":
                // 광부 + 대장장이: 특수 무기 제작 레시피 해금
                player.sendMessage("§d[조합 효과] 특수 무기 제작 레시피가 해금되었습니다!");
                // 실제 효과: 상점에 특수 아이템 추가 또는 제작 가능 플래그
                break;

            case "farmer_chef":
                // 농부 + 요리사: 특수 요리 레시피
                player.sendMessage("§d[조합 효과] 고급 요리 레시피가 해금되었습니다!");
                plugin.getTraitManager().addPoint(player, "chef", 1);
                break;

            case "explorer_fisher":
                // 탐험가 + 어부: 해상 지도 제작
                player.sendMessage("§d[조합 효과] 해상 워프 포인트가 추가되었습니다!");
                break;

            default:
                // 기본 효과: 특성 포인트 보너스
                String activeJob = plugin.getJobManager().getActiveJob(player);
                if (activeJob != null) {
                    plugin.getTraitManager().addPoint(player, activeJob, 2);
                    player.sendMessage("§d[조합 효과] 특성 포인트 +2!");
                }
                break;
        }
    }

    /**
     * 조합 보너스 계산 (예: 경험치 보너스, 드롭률 증가 등)
     */
    public double getComboBonus(Player player, String bonusType) {
        double bonus = 0.0;

        for (String comboKey : getUnlockedCombos(player)) {
            switch (comboKey) {
                case "miner_blacksmith":
                    if ("mining_speed".equals(bonusType)) bonus += 0.1; // +10%
                    if ("durability".equals(bonusType)) bonus += 0.15; // +15%
                    break;

                case "farmer_chef":
                    if ("crop_yield".equals(bonusType)) bonus += 0.2; // +20%
                    if ("cooking_quality".equals(bonusType)) bonus += 0.25; // +25%
                    break;

                case "explorer_fisher":
                    if ("rare_catch".equals(bonusType)) bonus += 0.15; // +15%
                    if ("movement_speed".equals(bonusType)) bonus += 0.1; // +10%
                    break;
            }
        }

        return bonus;
    }

    private void loadCombos() {
        java.io.File playerDir = new java.io.File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) return;
        java.io.File[] files = playerDir.listFiles((d,n)->n.endsWith(".yml"));
        if (files == null) return;
        for (java.io.File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml",""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                List<String> list = cfg.getStringList("combos.unlocked");
                if (!list.isEmpty()) unlockedCombos.put(uuid, new HashSet<>(list));
            } catch (Exception ignored) {}
        }
    }

    private void savePlayerCombosAsync(UUID uuid) {
        YamlConfiguration cfg = dataUtil.loadPlayerConfig(uuid);
        cfg.set("combos.unlocked", new ArrayList<>(unlockedCombos.getOrDefault(uuid, Collections.emptySet())));
        dataUtil.savePlayerConfigAsync(uuid, cfg);
    }

    public void saveAllAsync() {
        for (UUID u : unlockedCombos.keySet()) savePlayerCombosAsync(u);
    }

    public void saveAll() {
        for (UUID u : unlockedCombos.keySet()) {
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(u);
            cfg.set("combos.unlocked", new ArrayList<>(unlockedCombos.getOrDefault(u, Collections.emptySet())));
            dataUtil.savePlayerConfigSync(u, cfg);
        }
    }
}