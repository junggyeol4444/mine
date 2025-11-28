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

import java.util.Map;
import java.util.UUID;

/**
 * EquipmentManager — 시작 장비 지급, 업그레이드, 판매(Shop GUI 통해) 담당
 *
 * - 모든 시작 장비는 내구도 무한(unbreakable)
 * - 업그레이드: 아이템의 PDC에 upgrade_level 저장, 업그레이드 레벨에 따라 인챈트 부여
 *
 * 주의: Enchantment 상수는 서버 API 버전에 따라 차이가 있으므로 런타임에서 이름으로 조회하도록 구현했습니다.
 */
public class EquipmentManager {

    private final JobPlugin plugin;
    private final PluginDataUtil dataUtil;
    private final NamespacedKey upgradeKey;
    private final NamespacedKey starterKey;

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
        int maxLevel = plugin.getConfig().getInt("equipment.max_upgrade_level", 5);
        if (next > maxLevel) {
            player.sendMessage("§c이미 최대 업그레이드 단계입니다.");
            return false;
        }
        int base = plugin.getConfig().getInt("equipment.upgrade_base_price", 100);
        double mult = plugin.getConfig().getDouble("equipment.upgrade_multiplier", 1.8);
        int cost = (int) Math.round(base * Math.pow(mult, level));

        if (!plugin.getEconomyManager().takeMoney(player, cost)) {
            player.sendMessage("§c돈이 부족합니다. 업그레이드 비용: " + cost + plugin.getConfig().getString("economy.currency_symbol", "원"));
            return false;
        }

        // 주요 인챈트 결정 및 적용 (API 호환성 고려)
        Enchantment primary = selectPrimaryEnchantment(hand);
        int enchLevel = Math.min(next, plugin.getConfig().getInt("equipment.max_enchant_level", 5));
        if (primary != null) {
            try { im.addEnchant(primary, enchLevel, true); } catch (Exception ignored) {}
        }

        // Unbreaking 계열 인챈트(내구도 무한 대신 안전하게 추가) - 여러 후보 이름 시도
        Enchantment unb = findEnchantment("DURABILITY", "UNBREAKING");
        if (unb != null) {
            try { im.addEnchant(unb, Math.min(3, enchLevel), true); } catch (Exception ignored) {}
        }

        im.setUnbreakable(true);
        im.getPersistentDataContainer().set(upgradeKey, PersistentDataType.INTEGER, next);
        hand.setItemMeta(im);

        player.sendMessage("§a업그레이드 성공! 레벨: " + next + " (비용: " + cost + plugin.getConfig().getString("economy.currency_symbol", "원") + ")");
        return true;
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

    // 아이템 타입에 맞는 주요 인챈트 결정 (이름 후보로 조회)
    private Enchantment selectPrimaryEnchantment(ItemStack item) {
        String name = item.getType().name();
        if (name.contains("SWORD") || name.contains("AXE")) return findEnchantment("DAMAGE_ALL", "SHARPNESS");
        if (name.contains("BOW")) return findEnchantment("ARROW_DAMAGE", "ARROW_DAMAGE");
        if (name.contains("PICKAXE") || name.contains("SHOVEL") || name.contains("AXE")) return findEnchantment("DIG_SPEED", "EFFICIENCY");
        if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS")) return findEnchantment("PROTECTION_ENVIRONMENTAL", "PROTECTION");
        return findEnchantment("DURABILITY", "UNBREAKING");
    }
}