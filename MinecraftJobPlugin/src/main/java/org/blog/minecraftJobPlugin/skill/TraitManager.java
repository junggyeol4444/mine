package org.blog.minecraftJobPlugin.skill;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TraitManager {
    private final JobPlugin plugin;
    private final PluginDataUtil dataUtil;
    private final Map<UUID, Map<String, Set<String>>> traitChoices = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> traitPoints = new HashMap<>();

    public TraitManager(JobPlugin plugin) {
        this.plugin = plugin;
        this.dataUtil = new PluginDataUtil(plugin);
        loadTraitData();
    }

    public List<TraitMeta> getTraits(String job) {
        YamlConfiguration yaml = dataUtil.loadGlobal("traits");
        if (yaml == null) return Collections.emptyList();
        List<TraitMeta> list = new ArrayList<>();
        List<?> nodes = yaml.getList("traits." + job, Collections.emptyList());
        for (Object o : nodes) if (o instanceof Map) list.add(TraitMeta.fromYaml((Map<?,?>)o));
        return list;
    }

    public boolean selectTrait(Player p, String job, String traitName) {
        int pt = getPoints(p, job);
        if (pt <= 0) return false;
        traitChoices.computeIfAbsent(p.getUniqueId(), k->new HashMap<>()).computeIfAbsent(job, k->new HashSet<>()).add(traitName);
        traitPoints.computeIfAbsent(p.getUniqueId(), k->new HashMap<>()).put(job, pt-1);
        savePlayerTraitsAsync(p.getUniqueId());
        p.sendMessage("§b특성 선택: " + traitName);
        return true;
    }

    public int getPoints(Player p, String job) {
        return traitPoints.computeIfAbsent(p.getUniqueId(), k->new HashMap<>()).getOrDefault(job, 0);
    }

    public void addPoint(Player p, String job, int amount) {
        Map<String,Integer> map = traitPoints.computeIfAbsent(p.getUniqueId(), k->new HashMap<>());
        map.put(job, map.getOrDefault(job,0) + amount);
        savePlayerTraitsAsync(p.getUniqueId());
    }

    private void loadTraitData() {
        java.io.File playerDir = new java.io.File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) return;
        java.io.File[] files = playerDir.listFiles((d,n)->n.endsWith(".yml"));
        if (files == null) return;
        for (java.io.File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml",""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                Map<String, Set<String>> choices = new HashMap<>();
                Map<String, Integer> points = new HashMap<>();
                if (cfg.isConfigurationSection("traits")) {
                    for (String job : cfg.getConfigurationSection("traits").getKeys(false)) {
                        List<String> chosen = cfg.getStringList("traits." + job + ".chosen");
                        if (!chosen.isEmpty()) choices.put(job, new HashSet<>(chosen));
                        int pt = cfg.getInt("traits." + job + ".points", 0);
                        if (pt > 0) points.put(job, pt);
                    }
                }
                if (!choices.isEmpty()) traitChoices.put(uuid, choices);
                if (!points.isEmpty()) traitPoints.put(uuid, points);
            } catch (Exception ignored) {}
        }
    }

    private void savePlayerTraitsAsync(UUID uuid) {
        YamlConfiguration cfg = dataUtil.loadPlayerConfig(uuid);
        Map<String, Set<String>> choices = traitChoices.getOrDefault(uuid, Collections.emptyMap());
        Map<String, Integer> points = traitPoints.getOrDefault(uuid, Collections.emptyMap());
        for (Map.Entry<String, Set<String>> e : choices.entrySet()) cfg.set("traits." + e.getKey() + ".chosen", new ArrayList<>(e.getValue()));
        for (Map.Entry<String, Integer> e : points.entrySet()) cfg.set("traits." + e.getKey() + ".points", e.getValue());
        dataUtil.savePlayerConfigAsync(uuid, cfg);
    }

    public void saveAllAsync() {
        for (UUID u : traitChoices.keySet()) savePlayerTraitsAsync(u);
        for (UUID u : traitPoints.keySet()) savePlayerTraitsAsync(u);
    }

    public void saveAll() {
        for (UUID u : traitChoices.keySet()) {
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(u);
            Map<String, Set<String>> choices = traitChoices.getOrDefault(u, Collections.emptyMap());
            Map<String, Integer> points = traitPoints.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String, Set<String>> e : choices.entrySet()) cfg.set("traits." + e.getKey() + ".chosen", new ArrayList<>(e.getValue()));
            for (Map.Entry<String, Integer> e : points.entrySet()) cfg.set("traits." + e.getKey() + ".points", e.getValue());
            dataUtil.savePlayerConfigSync(u, cfg);
        }
    }
}