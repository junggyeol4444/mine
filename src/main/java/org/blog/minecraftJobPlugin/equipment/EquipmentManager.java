package org.blog.minecraftJobPlugin.equipment;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.job.JobMeta;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Material;

import java.util.*;

/**
 * EquipmentManager — 개선된 업그레이드 시스템
 * - 매 업그레이드마다 랜덤하게 하나의 인챈트만 1레벨씩 증가
 * - 내구성 인챈트는 제외
 * - 최대 업그레이드 레벨: 20
 */
public class EquipmentManager {

    private final JobPlugin plugin;
    private final PluginDataUtil dataUtil;
    private final NamespacedKey upgradeKey;
    private final NamespacedKey starterKey;
    private final Random random = new Random();

    public EquipmentManager(JobPlugin plugin) {
        this.plugin = plugin;
        this.dataUtil = new PluginDataUtil(plugin);
        this.upgradeKey = new NamespacedKey(plugin, "upgrade_level");
        this.starterKey = new NamespacedKey(plugin, "is_starter_item");
    }

    public void giveStartingItems(Player player, String jobKey) {
        JobMeta meta = plugin.getJobManager().getJobMeta(jobKey);
        if (meta == null) return;
        for (JobMeta.StartingItem si : meta.startingItems) {
            ItemStack item = new ItemStack(si.material, Math.max(1, si.amount));
            ItemMeta im = item.getItemMeta();
            if (si.displayName != null && !si.displayName.isBlank()) im.setDisplayName(si.displayName);
            im.setUnbreakable(true);
            im.getPersistentDataContainer().set(upgradeKey, PersistentDataType.INTEGER, 0);
            im.getPersistentDataContainer().set(starterKey, PersistentDataType.INTEGER, 1);
            if (si.enchants != null) {
                for (Map.Entry<String, Object> e : si.enchants.entrySet()) {
                    String enchName = e.getKey();
                    int lvl = 1;
                    try { lvl = ((Number)e.getValue()).intValue(); } catch (Exception ignored) {}
                    Enchantment ench = Enchantment.getByName(enchName);
                    if (ench != null) im.addEnchant(ench, Math.max(1, lvl), true);
                }
            }
            item.setItemMeta(im);
            if (si.slot != null) {
                int s = Math.max(0, Math.min(35, si.slot));
                player.getInventory().setItem(s, item);
            } else {
                player.getInventory().addItem(item);
            }
        }
        player.sendMessage("§a[" + jobKey + "] 시작 장비를 지급했습니다.");
    }

    /**
     * 업그레이드 로직 개선
     * - 랜덤하게 하나의 인챈트만 1레벨 증가
     * - 내구성 제외
     * - 최대 레벨 20
     */
    public boolean upgradeItemInHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage("§c손에 업그레이드할 아이템이 없습니다.");
            return false;
        }
        
        ItemMeta im = hand.getItemMeta();
        if (im == null) {
            player.sendMessage("§c이 아이템은 업그레이드할 수 없습니다.");
            return false;
        }
        
        Integer cur = im.getPersistentDataContainer().get(upgradeKey, PersistentDataType.INTEGER);
        int level = (cur == null) ? 0 : cur;
        int next = level + 1;
        
        int maxLevel = plugin.getConfig().getInt("equipment.max_upgrade_level", 20);
        if (next > maxLevel) {
            player.sendMessage("§c이미 최대 업그레이드 단계입니다. (최대: " + maxLevel + ")");
            return false;
        }
        
        // 비용 계산
        int base = plugin.getConfig().getInt("equipment.upgrade_base_price", 100);
        double mult = plugin.getConfig().getDouble("equipment.upgrade_multiplier", 1.8);
        int cost = (int) Math.round(base * Math.pow(mult, level));

        if (!plugin.getEconomyManager().takeMoney(player, cost)) {
            player.sendMessage("§c돈이 부족합니다. 업그레이드 비용: " + cost + plugin.getConfig().getString("economy.currency_symbol", "원"));
            return false;
        }

        // 아이템에 적용 가능한 모든 인챈트 수집 (내구성 제외)
        List<Enchantment> availableEnchants = getAvailableEnchantments(hand);
        
        if (availableEnchants.isEmpty()) {
            player.sendMessage("§c이 아이템에 적용할 수 있는 인챈트가 없습니다.");
            plugin.getEconomyManager().addMoney(player, cost); // 환불
            return false;
        }

        // 랜덤하게 하나 선택
        Enchantment selected = availableEnchants.get(random.nextInt(availableEnchants.size()));
        int currentEnchLevel = im.getEnchantLevel(selected);
        int newEnchLevel = currentEnchLevel + 1;
        
        // 인챈트 최대 레벨 체크 (일부 인챈트는 자체 최대치가 있음)
        int enchMaxLevel = plugin.getConfig().getInt("equipment.max_enchant_level_per_type", 10);
        if (newEnchLevel > enchMaxLevel) {
            newEnchLevel = enchMaxLevel;
        }
        
        // 인챈트 적용
        im.addEnchant(selected, newEnchLevel, true);
        
        // 업그레이드 레벨 저장
        im.setUnbreakable(true);
        im.getPersistentDataContainer().set(upgradeKey, PersistentDataType.INTEGER, next);
        hand.setItemMeta(im);

        player.sendMessage("§a━━━━━━ 업그레이드 성공 ━━━━━━");
        player.sendMessage("§f업그레이드 레벨: §e" + level + " §f→ §6" + next);
        player.sendMessage("§f강화된 인챈트: §d" + getEnchantmentName(selected) + " " + romanNumeral(newEnchLevel));
        player.sendMessage("§f비용: §c" + cost + plugin.getConfig().getString("economy.currency_symbol", "원"));
        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━");
        
        return true;
    }

    /**
     * 아이템에 적용 가능한 모든 인챈트 반환 (내구성 제외)
     */
    private List<Enchantment> getAvailableEnchantments(ItemStack item) {
        List<Enchantment> result = new ArrayList<>();
        String name = item.getType().name();
        
        // 내구성 인챈트 (제외할 것)
        Enchantment unbreaking = findEnchantment("DURABILITY", "UNBREAKING");
        Enchantment mending = findEnchantment("MENDING");
        
        // 무기류
        if (name.contains("SWORD")) {
            addIfNotNull(result, findEnchantment("DAMAGE_ALL", "SHARPNESS"));
            addIfNotNull(result, findEnchantment("DAMAGE_UNDEAD", "SMITE"));
            addIfNotNull(result, findEnchantment("DAMAGE_ARTHROPODS", "BANE_OF_ARTHROPODS"));
            addIfNotNull(result, findEnchantment("FIRE_ASPECT"));
            addIfNotNull(result, findEnchantment("KNOCKBACK"));
            addIfNotNull(result, findEnchantment("LOOT_BONUS_MOBS", "LOOTING"));
            addIfNotNull(result, findEnchantment("SWEEPING_EDGE", "SWEEPING"));
        }
        
        // 도끼
        if (name.contains("AXE")) {
            addIfNotNull(result, findEnchantment("DAMAGE_ALL", "SHARPNESS"));
            addIfNotNull(result, findEnchantment("DAMAGE_UNDEAD", "SMITE"));
            addIfNotNull(result, findEnchantment("DAMAGE_ARTHROPODS", "BANE_OF_ARTHROPODS"));
            addIfNotNull(result, findEnchantment("DIG_SPEED", "EFFICIENCY"));
            addIfNotNull(result, findEnchantment("LOOT_BONUS_BLOCKS", "FORTUNE"));
            addIfNotNull(result, findEnchantment("SILK_TOUCH"));
        }
        
        // 곡괭이, 삽
        if (name.contains("PICKAXE") || name.contains("SHOVEL")) {
            addIfNotNull(result, findEnchantment("DIG_SPEED", "EFFICIENCY"));
            addIfNotNull(result, findEnchantment("LOOT_BONUS_BLOCKS", "FORTUNE"));
            addIfNotNull(result, findEnchantment("SILK_TOUCH"));
        }
        
        // 활
        if (name.contains("BOW")) {
            addIfNotNull(result, findEnchantment("ARROW_DAMAGE", "POWER"));
            addIfNotNull(result, findEnchantment("ARROW_KNOCKBACK", "PUNCH"));
            addIfNotNull(result, findEnchantment("ARROW_FIRE", "FLAME"));
            addIfNotNull(result, findEnchantment("ARROW_INFINITE", "INFINITY"));
        }
        
        // 석궁
        if (name.contains("CROSSBOW")) {
            addIfNotNull(result, findEnchantment("QUICK_CHARGE"));
            addIfNotNull(result, findEnchantment("MULTISHOT"));
            addIfNotNull(result, findEnchantment("PIERCING"));
        }
        
        // 낚싯대
        if (name.contains("FISHING_ROD")) {
            addIfNotNull(result, findEnchantment("LURE"));
            addIfNotNull(result, findEnchantment("LUCK_OF_THE_SEA", "LUCK"));
        }
        
        // 헬멧
        if (name.contains("HELMET")) {
            addIfNotNull(result, findEnchantment("PROTECTION_ENVIRONMENTAL", "PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_FIRE", "FIRE_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_EXPLOSIONS", "BLAST_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION"));
            addIfNotNull(result, findEnchantment("OXYGEN", "RESPIRATION"));
            addIfNotNull(result, findEnchantment("WATER_WORKER", "AQUA_AFFINITY"));
        }
        
        // 흉갑
        if (name.contains("CHESTPLATE")) {
            addIfNotNull(result, findEnchantment("PROTECTION_ENVIRONMENTAL", "PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_FIRE", "FIRE_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_EXPLOSIONS", "BLAST_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION"));
        }
        
        // 레깅스
        if (name.contains("LEGGINGS")) {
            addIfNotNull(result, findEnchantment("PROTECTION_ENVIRONMENTAL", "PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_FIRE", "FIRE_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_EXPLOSIONS", "BLAST_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION"));
            addIfNotNull(result, findEnchantment("SWIFT_SNEAK"));
        }
        
        // 부츠
        if (name.contains("BOOTS")) {
            addIfNotNull(result, findEnchantment("PROTECTION_ENVIRONMENTAL", "PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_FIRE", "FIRE_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_EXPLOSIONS", "BLAST_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION"));
            addIfNotNull(result, findEnchantment("PROTECTION_FALL", "FEATHER_FALLING"));
            addIfNotNull(result, findEnchantment("DEPTH_STRIDER"));
            addIfNotNull(result, findEnchantment("FROST_WALKER"));
            addIfNotNull(result, findEnchantment("SOUL_SPEED"));
        }
        
        // 삼지창
        if (name.contains("TRIDENT")) {
            addIfNotNull(result, findEnchantment("IMPALING"));
            addIfNotNull(result, findEnchantment("LOYALTY"));
            addIfNotNull(result, findEnchantment("RIPTIDE"));
            addIfNotNull(result, findEnchantment("CHANNELING"));
        }
        
        // 내구성 관련 제외
        result.remove(unbreaking);
        result.remove(mending);
        
        return result;
    }

    private void addIfNotNull(List<Enchantment> list, Enchantment ench) {
        if (ench != null) list.add(ench);
    }

    // 판매 가격 조회 도움 (ShopGUI에서 사용)
    public int getSellPriceForMaterial(String job, Material mat) {
        YamlConfiguration shops = dataUtil.loadGlobal("shops");
        int price = 0;
        if (shops != null) {
            if (job != null && !job.isBlank()) price = shops.getInt("shops." + job + ".sell." + mat.name(), 0);
            if (price <= 0) price = shops.getInt("shops.global.sell." + mat.name(), 0);
        }
        if (price <= 0) price = plugin.getConfig().getInt("equipment.default_sell_price", 10);
        return price;
    }

    public void saveAll() {
        // 현재는 PDC에 정보 보관하므로 별도 저장 없음
    }

    // 여러 후보 이름을 시도해서 Enchantment 반환 (API 버전 차이 대응)
    private Enchantment findEnchantment(String... names) {
        for (String n : names) {
            if (n == null) continue;
            Enchantment e = Enchantment.getByName(n);
            if (e != null) return e;
        }
        return null;
    }
    
    // 인챈트 이름 한글화
    private String getEnchantmentName(Enchantment ench) {
        String key = ench.getKey().getKey().toUpperCase();
        Map<String, String> names = new HashMap<>();
        names.put("SHARPNESS", "날카로움");
        names.put("SMITE", "강타");
        names.put("BANE_OF_ARTHROPODS", "살충");
        names.put("EFFICIENCY", "효율");
        names.put("FORTUNE", "행운");
        names.put("SILK_TOUCH", "섬세한 손길");
        names.put("POWER", "힘");
        names.put("PUNCH", "밀치기");
        names.put("FLAME", "화염");
        names.put("INFINITY", "무한");
        names.put("PROTECTION", "보호");
        names.put("FIRE_PROTECTION", "화염 보호");
        names.put("BLAST_PROTECTION", "폭발 보호");
        names.put("PROJECTILE_PROTECTION", "발사체 보호");
        names.put("FEATHER_FALLING", "가벼운 착지");
        names.put("RESPIRATION", "호흡");
        names.put("AQUA_AFFINITY", "물갈퀴");
        names.put("DEPTH_STRIDER", "물갈퀴");
        names.put("FROST_WALKER", "차가운 걸음");
        names.put("LOOTING", "약탈");
        names.put("FIRE_ASPECT", "발화");
        names.put("KNOCKBACK", "밀치기");
        return names.getOrDefault(key, ench.getKey().getKey());
    }
    
    // 로마 숫자 변환
    private String romanNumeral(int num) {
        if (num <= 0) return "0";
        if (num >= 20) return String.valueOf(num);
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                          "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        return romans[num];
    }
}