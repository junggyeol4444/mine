package org.blog.minecraftJobPlugin.skill;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobManager;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * SkillManager — 모든 스킬 구현
 */
public class SkillManager {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final PluginDataUtil dataUtil;

    private final Map<UUID, Map<String, Integer>> skillLevels = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> skillExp = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    // 워프 포인트 저장
    private final Map<UUID, Map<String, Location>> warpPoints = new HashMap<>();

    // 버프 활성화 저장 (간단한 방식)
    private final Map<UUID, Map<String, Long>> activeBuffs = new HashMap<>();

    public SkillManager(JobPlugin plugin, JobManager jm) {
        this.plugin = plugin;
        this.jobManager = jm;
        this.dataUtil = new PluginDataUtil(plugin);
        loadSkillData();
    }

    public boolean useSkill(Player player, String skillName) {
        UUID id = player.getUniqueId();
        int level = getSkillLevel(player, skillName);
        long now = System.currentTimeMillis();
        long cd = SkillMeta.loadFromConfig(skillName, plugin).getCooldown(level) * 1000L;
        cooldowns.computeIfAbsent(id, k->new HashMap<>());
        long last = cooldowns.get(id).getOrDefault(skillName, 0L);

        if (now - last < cd) {
            long remaining = (cd - (now - last)) / 1000;
            player.sendMessage("§c스킬 쿨타임 중입니다. (남은 시간: " + remaining + "초)");
            return false;
        }
        cooldowns.get(id).put(skillName, now);

        // 등급 보너스 적용
        String job = jobManager.getActiveJob(player);
        double expBonus = job != null ? plugin.getGradeManager().getSkillExpBonus(player, job) : 0.0;
        int baseExp = 20;
        int bonusExp = (int)(baseExp * expBonus);
        addSkillExp(player, skillName, baseExp + bonusExp);

        // 스킬별 효과 실행
        executeSkillEffect(player, skillName, level);

        return true;
    }

    /**
     * 스킬 효과 실행
     */
    private void executeSkillEffect(Player player, String skillName, int level) {
        Location loc = player.getLocation();

        switch (skillName) {
            case "geoScan":
                // 지형 스캔 - 주변 광물 하이라이트
                int radius = 5 + (level * 2);
                player.sendMessage("§6[GeoScan] §f반경 " + radius + "블록 스캔 중...");
                scanOres(player, radius);
                break;

            case "warpPoint":
                // 워프 지점 설치
                setWarpPoint(player, level);
                break;

            case "fastLumber":
                // 빠른 벌목 - 채굴 속도 버프
                int duration = 10 + (level * 5);
                int amplifier = level - 1;
                PotionEffectType haste = PotionEffectType.getByName("FAST_DIGGING");
                if (haste == null) haste = PotionEffectType.getByName("HASTE");
                if (haste != null) {
                    player.addPotionEffect(new PotionEffect(haste, duration * 20, amplifier));
                    player.sendMessage("§a[FastLumber] §f벌목 속도 증가 (" + duration + "초)");
                }
                break;

            case "efficientCarve":
                // 효율적 조각 - 목재 드롭 증가 버프
                activateBuff(player, "efficientCarve");
                player.sendMessage("§a[EfficientCarve] §f목재 드롭 증가 버프 활성화! (60초)");
                break;

            case "veinDetect":
                // 광맥 감지
                int veinRadius = 8 + (level * 3);
                player.sendMessage("§e[VeinDetect] §f반경 " + veinRadius + "블록 광맥 감지...");
                detectVeins(player, veinRadius);
                break;

            case "blastMining":
                // 폭파 채굴 - 주변 블록 한번에 파괴
                blastMine(player, level);
                break;

            case "criticalShot":
                // 치명타 사격 - 다음 화살 데미지 증가
                activateBuff(player, "criticalShot");
                player.sendMessage("§c[CriticalShot] §f다음 공격 치명타 확률 증가! (20초)");
                break;

            case "tracking":
                // 추적 - 주변 몹 감지
                trackEntities(player, 20 + (level * 10));
                break;

            case "bumperCrop":
                // 풍작 - 수확량 증가 버프
                activateBuff(player, "bumperCrop");
                player.sendMessage("§a[BumperCrop] §f풍작 버프 활성화! 수확량 +" + (10 + level * 5) + "% (120초)");
                break;

            case "nutrientBoost":
                // 영양 강화 - 작물 성장 속도 증가
                boostCrops(player, 5 + level);
                break;

            case "rareCatch":
                // 희귀 입질 - 낚시 확률 증가
                activateBuff(player, "rareCatch");
                player.sendMessage("§b[RareCatch] §f희귀 낚시 확률 증가! (60초)");
                break;

            case "deepSea":
                // 심해 - 수중 호흡 + 야간 투시
                int deepDuration = 30 + (level * 10);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, deepDuration * 20, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, deepDuration * 20, 0));
                player.sendMessage("§b[DeepSea] §f심해 탐험 버프 활성화! (" + deepDuration + "초)");
                break;

            case "precisionForge":
                // 정밀 단조 - 다음 제작 품질 증가
                player.sendMessage("§6[PrecisionForge] §f다음 제작 품질 증가! (60초)");
                break;

            case "tempering":
                // 열처리 - 장비 내구도 회복
                temperEquipment(player, level);
                break;

            case "doubleBrew":
                // 이중 증류 - 포션 효과 증가
                player.sendMessage("§d[DoubleBrew] §f다음 포션 효과 2배! (60초)");
                break;

            case "elixirCraft":
                // 엘릭서 제작 - 특수 효과
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 30, level - 1));
                PotionEffectType resistance = PotionEffectType.getByName("RESISTANCE");
                if (resistance == null) resistance = PotionEffectType.getByName("DAMAGE_RESISTANCE");
                if (resistance != null) {
                    player.addPotionEffect(new PotionEffect(resistance, 20 * 30, 0));
                }
                player.sendMessage("§d[ElixirCraft] §f생명의 엘릭서 효과! (30초)");
                break;

            case "gourmetCook":
                // 명장 요리 - 음식 버프
                player.sendMessage("§e[GourmetCook] §f다음 음식 버프 강화! (60초)");
                break;

            case "spiceMaster":
                // 양념 마스터 - 포만감 회복
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.sendMessage("§e[SpiceMaster] §f포만감 완전 회복!");
                break;

            default:
                player.sendMessage("§d[" + skillName + "] §f스킬을 사용했습니다.");
        }
    }

    /**
     * 광물 스캔
     */
    private void scanOres(Player player, int radius) {
        Location center = player.getLocation();
        int found = 0;
        Set<Material> ores = new HashSet<>(Arrays.asList(
                Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE,
                Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE,
                Material.REDSTONE_ORE, Material.COPPER_ORE,
                Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE
        ));

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (ores.contains(block.getType())) {
                        block.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0);
                        found++;
                    }
                }
            }
        }
        player.sendMessage("§6→ §f광물 " + found + "개 발견!");
    }

    /**
     * 워프 포인트 설정
     */
    private void setWarpPoint(Player player, int level) {
        UUID id = player.getUniqueId();
        warpPoints.computeIfAbsent(id, k -> new HashMap<>());
        Map<String, Location> points = warpPoints.get(id);

        int maxPoints = level;
        if (points.size() >= maxPoints) {
            player.sendMessage("§c워프 포인트가 가득 찼습니다. /skill warp list 로 확인하세요.");
            return;
        }

        String pointName = "warp_" + (points.size() + 1);
        points.put(pointName, player.getLocation());
        player.sendMessage("§a[WarpPoint] §f워프 포인트 설정: " + pointName);
        player.sendMessage("§7/skill warp " + pointName + " 로 순간이동");
    }

    /**
     * 광맥 감지
     */
    private void detectVeins(Player player, int radius) {
        Map<Material, Integer> oreCount = new HashMap<>();
        Location center = player.getLocation();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Material type = center.clone().add(x, y, z).getBlock().getType();
                    if (type.name().contains("ORE")) {
                        oreCount.put(type, oreCount.getOrDefault(type, 0) + 1);
                    }
                }
            }
        }

        if (oreCount.isEmpty()) {
            player.sendMessage("§e→ §7주변에 광맥이 없습니다.");
        } else {
            player.sendMessage("§e→ §f감지된 광맥:");
            oreCount.forEach((ore, count) -> {
                player.sendMessage("  §6" + ore.name() + " §fx" + count);
            });
        }
    }

    /**
     * 폭파 채굴
     */
    private void blastMine(Player player, int level) {
        Location center = player.getTargetBlock(null, 5).getLocation();
        int radius = 1 + (level / 2);

        player.sendMessage("§c[BlastMining] §f폭파 채굴! (반경 " + radius + ")");

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType().name().contains("ORE") ||
                            block.getType() == Material.STONE ||
                            block.getType() == Material.COBBLESTONE ||
                            block.getType() == Material.DEEPSLATE) {
                        block.breakNaturally(player.getInventory().getItemInMainHand());
                    }
                }
            }
        }

        // 폭발 파티클 (버전 호환)
        try {
            center.getWorld().spawnParticle(Particle.valueOf("EXPLOSION_LARGE"), center, 3, 1, 1, 1, 0);
        } catch (IllegalArgumentException e) {
            try {
                center.getWorld().spawnParticle(Particle.valueOf("EXPLOSION"), center, 3, 1, 1, 1, 0);
            } catch (IllegalArgumentException ex) {
                // 파티클 없음
            }
        }
    }

    /**
     * 몹 추적
     */
    private void trackEntities(Player player, int radius) {
        player.sendMessage("§c[Tracking] §f주변 생명체 추적...");
        player.getNearbyEntities(radius, radius, radius).stream()
                .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
                .filter(e -> !(e instanceof Player))
                .forEach(e -> {
                    Location loc = e.getLocation();
                    int distance = (int) player.getLocation().distance(loc);
                    player.sendMessage("  §7" + e.getType().name() + " §f- " + distance + "m");
                    // 파티클 (버전 호환)
                    try {
                        e.getWorld().spawnParticle(Particle.valueOf("VILLAGER_ANGRY"), loc.add(0, 1, 0), 5);
                    } catch (IllegalArgumentException ex) {
                        try {
                            e.getWorld().spawnParticle(Particle.valueOf("ANGRY_VILLAGER"), loc.add(0, 1, 0), 5);
                        } catch (IllegalArgumentException ex2) {
                            // 파티클 없음
                        }
                    }
                });
    }

    /**
     * 작물 성장 촉진
     */
    private void boostCrops(Player player, int radius) {
        Location center = player.getLocation();
        int boosted = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getBlockData() instanceof org.bukkit.block.data.Ageable) {
                        org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) block.getBlockData();
                        if (ageable.getAge() < ageable.getMaximumAge()) {
                            ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + 1));
                            block.setBlockData(ageable);
                            boosted++;
                        }
                    }
                }
            }
        }

        player.sendMessage("§a[NutrientBoost] §f작물 " + boosted + "개 성장 촉진!");
    }

    /**
     * 장비 내구도 회복
     */
    private void temperEquipment(Player player, int level) {
        int repaired = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType().getMaxDurability() > 0) {
                org.bukkit.inventory.meta.Damageable meta = (org.bukkit.inventory.meta.Damageable) item.getItemMeta();
                if (meta != null && meta.hasDamage()) {
                    int damage = meta.getDamage();
                    int repair = Math.min(damage, item.getType().getMaxDurability() / (6 - level));
                    meta.setDamage(Math.max(0, damage - repair));
                    item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) meta);
                    repaired++;
                }
            }
        }
        player.sendMessage("§6[Tempering] §f장비 " + repaired + "개 수리 완료!");
    }

    public int getSkillLevel(Player player, String skill) {
        return skillLevels.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).getOrDefault(skill, 1);
    }

    public int getSkillExp(Player player, String skill) {
        return skillExp.computeIfAbsent(player.getUniqueId(), k->new HashMap<>()).getOrDefault(skill, 0);
    }

    private void addSkillExp(Player player, String skill, int exp) {
        UUID id = player.getUniqueId();
        skillExp.computeIfAbsent(id, k->new HashMap<>());
        int before = getSkillExp(player, skill);
        int after = before + exp;
        skillExp.get(id).put(skill, after);

        int curLevel = getSkillLevel(player, skill);
        SkillMeta meta = SkillMeta.loadFromConfig(skill, plugin);
        if (after >= meta.getExpForLevel(curLevel + 1)) {
            skillLevels.computeIfAbsent(id, k->new HashMap<>()).put(skill, curLevel + 1);
            player.sendMessage("§d[" + skill + "] §f레벨업! Lv." + (curLevel + 1));
        }
    }

    private void loadSkillData() {
        java.io.File playerDir = new java.io.File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) return;
        java.io.File[] files = playerDir.listFiles((d,n)->n.endsWith(".yml"));
        if (files == null) return;
        for (java.io.File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml",""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                if (cfg.isConfigurationSection("skills")) {
                    Map<String,Integer> lv = new HashMap<>();
                    Map<String,Integer> ex = new HashMap<>();
                    for (String s : cfg.getConfigurationSection("skills").getKeys(false)) {
                        lv.put(s, cfg.getInt("skills." + s + ".level", 1));
                        ex.put(s, cfg.getInt("skills." + s + ".exp", 0));
                    }
                    if (!lv.isEmpty()) skillLevels.put(uuid, lv);
                    if (!ex.isEmpty()) skillExp.put(uuid, ex);
                }
            } catch (Exception ignored) {}
        }
    }

    private void savePlayerSkillsSync(UUID uuid) {
        YamlConfiguration cfg = dataUtil.loadPlayerConfig(uuid);
        Map<String,Integer> lv = skillLevels.getOrDefault(uuid, Collections.emptyMap());
        Map<String,Integer> ex = skillExp.getOrDefault(uuid, Collections.emptyMap());
        for (Map.Entry<String,Integer> e : lv.entrySet()) cfg.set("skills." + e.getKey() + ".level", e.getValue());
        for (Map.Entry<String,Integer> e : ex.entrySet()) cfg.set("skills." + e.getKey() + ".exp", e.getValue());
        dataUtil.savePlayerConfigSync(uuid, cfg);
    }

    public void saveAllAsync() {
        Set<UUID> all = new HashSet<>();
        all.addAll(skillLevels.keySet());
        all.addAll(skillExp.keySet());
        for (UUID u : all) {
            YamlConfiguration cfg = dataUtil.loadPlayerConfig(u);
            Map<String,Integer> lv = skillLevels.getOrDefault(u, Collections.emptyMap());
            Map<String,Integer> ex = skillExp.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String,Integer> e : lv.entrySet()) cfg.set("skills." + e.getKey() + ".level", e.getValue());
            for (Map.Entry<String,Integer> e : ex.entrySet()) cfg.set("skills." + e.getKey() + ".exp", e.getValue());
            dataUtil.savePlayerConfigAsync(u, cfg);
        }
    }

    public void saveAll() {
        Set<UUID> all = new HashSet<>();
        all.addAll(skillLevels.keySet());
        all.addAll(skillExp.keySet());
        for (UUID u : all) savePlayerSkillsSync(u);
    }

    public SkillMeta getSkillMeta(String skill) {
        return SkillMeta.loadFromConfig(skill, plugin);
    }

    /**
     * 버프 활성화
     */
    private void activateBuff(Player player, String buffName) {
        UUID id = player.getUniqueId();
        activeBuffs.computeIfAbsent(id, k -> new HashMap<>()).put(buffName, System.currentTimeMillis());
    }

    /**
     * 버프 활성 여부 확인
     */
    public boolean hasActiveBuff(Player player, String buffName) {
        UUID id = player.getUniqueId();
        Long time = activeBuffs.getOrDefault(id, Collections.emptyMap()).get(buffName);
        if (time == null) return false;

        long duration = buffName.equals("bumperCrop") ? 120000 : 60000; // bumperCrop은 120초
        return (System.currentTimeMillis() - time) < duration;
    }
}