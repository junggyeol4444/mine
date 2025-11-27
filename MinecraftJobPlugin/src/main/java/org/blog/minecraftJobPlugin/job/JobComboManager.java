package org.blog.minecraftJobPlugin.job;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.UUID;

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

    public boolean hasCombo(Player player, String comboKey) {
        return unlockedCombos.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(comboKey);
    }

    public boolean unlockCombo(Player player, String comboKey) {
        unlockedCombos.computeIfAbsent(player.getUniqueId(), k->new HashSet<>()).add(comboKey);
        savePlayerCombosAsync(player.getUniqueId());
        player.sendMessage("§d직업 조합 해금: " + comboKey);
        return true;
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