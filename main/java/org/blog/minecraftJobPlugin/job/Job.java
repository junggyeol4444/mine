package org.blog.minecraftJobPlugin.job;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 직업 데이터 클래스
 */
public class Job {
    private final String name;
    private final String displayName;
    private final String description;
    private final List<String> skills;
    private final List<String> actions;
    private final String rarity;
    private final List<ItemStack> startingItems;
    private final ItemStack icon;

    public Job(String name, String displayName, String description,
               List<String> skills, List<String> actions, String rarity,
               List<ItemStack> startingItems, ItemStack icon) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.skills = skills != null ? skills : new ArrayList<>();
        this.actions = actions != null ? actions : new ArrayList<>();
        this.rarity = rarity != null ? rarity : "common";
        this.startingItems = startingItems != null ? startingItems : new ArrayList<>();
        this.icon = icon != null ? icon : createDefaultIcon(displayName, description);
    }

    /**
     * YAML 설정에서 Job 객체 생성
     */
    public static Job fromConfig(YamlConfiguration config, String jobKey) {
        String basePath = "jobs." + jobKey;

        String displayName = config.getString(basePath + ".display", jobKey);
        String description = config.getString(basePath + ".description", "");
        List<String> skills = config.getStringList(basePath + ".skills");
        List<String> actions = config.getStringList(basePath + ".actions");
        String rarity = config.getString(basePath + ".rarity", "common");

        // 시작 아이템 파싱
        List<ItemStack> startingItems = parseStartingItems(config, basePath);

        // 아이콘 생성
        ItemStack icon = createJobIcon(displayName, description, rarity);

        return new Job(jobKey, displayName, description, skills, actions, rarity, startingItems, icon);
    }

    /**
     * 시작 아이템 파싱
     */
    private static List<ItemStack> parseStartingItems(YamlConfiguration config, String basePath) {
        List<ItemStack> items = new ArrayList<>();

        if (!config.isList(basePath + ".startingItems")) {
            return items;
        }

        List<?> itemList = config.getList(basePath + ".startingItems");
        if (itemList == null) {
            return items;
        }

        for (Object obj : itemList) {
            if (!(obj instanceof Map)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) obj;

            // Material 파싱
            String materialName = String.valueOf(itemMap.getOrDefault("material", "STONE"));
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                material = Material.STONE;
            }

            // 수량
            int amount = 1;
            if (itemMap.containsKey("amount")) {
                try {
                    amount = ((Number) itemMap.get("amount")).intValue();
                } catch (Exception e) {
                    amount = 1;
                }
            }

            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                // 디스플레이 이름
                if (itemMap.containsKey("name")) {
                    String displayName = String.valueOf(itemMap.get("name"));
                    meta.setDisplayName(displayName);
                }

                // 인챈트
                if (itemMap.containsKey("enchants") && itemMap.get("enchants") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> enchants = (Map<String, Object>) itemMap.get("enchants");

                    for (Map.Entry<String, Object> entry : enchants.entrySet()) {
                        Enchantment enchant = Enchantment.getByName(entry.getKey().toUpperCase());
                        if (enchant != null) {
                            int level = 1;
                            try {
                                level = ((Number) entry.getValue()).intValue();
                            } catch (Exception e) {
                                level = 1;
                            }
                            meta.addEnchant(enchant, level, true);
                        }
                    }
                }

                item.setItemMeta(meta);
            }

            items.add(item);
        }

        return items;
    }

    /**
     * 직업 아이콘 생성
     */
    private static ItemStack createJobIcon(String displayName, String description, String rarity) {
        Material iconMaterial = Material.PAPER;

        // 희귀도에 따른 아이콘 변경
        switch (rarity.toLowerCase()) {
            case "common":
                iconMaterial = Material.PAPER;
                break;
            case "uncommon":
                iconMaterial = Material.BOOK;
                break;
            case "rare":
                iconMaterial = Material.ENCHANTED_BOOK;
                break;
            case "epic":
                iconMaterial = Material.WRITABLE_BOOK;
                break;
            case "legendary":
                iconMaterial = Material.KNOWLEDGE_BOOK;
                break;
        }

        ItemStack icon = new ItemStack(iconMaterial);
        ItemMeta meta = icon.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§b§l" + displayName);

            List<String> lore = new ArrayList<>();
            lore.add("§7" + description);
            lore.add("");
            lore.add("§e희귀도: §f" + getRarityColor(rarity) + capitalize(rarity));

            meta.setLore(lore);
            icon.setItemMeta(meta);
        }

        return icon;
    }

    /**
     * 기본 아이콘 생성
     */
    private static ItemStack createDefaultIcon(String displayName, String description) {
        ItemStack icon = new ItemStack(Material.PAPER);
        ItemMeta meta = icon.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§b§l" + displayName);
            List<String> lore = new ArrayList<>();
            lore.add("§7" + description);
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }

        return icon;
    }

    /**
     * 희귀도 색상 코드
     */
    private static String getRarityColor(String rarity) {
        switch (rarity.toLowerCase()) {
            case "common": return "§f";
            case "uncommon": return "§a";
            case "rare": return "§9";
            case "epic": return "§5";
            case "legendary": return "§6";
            default: return "§7";
        }
    }

    /**
     * 첫 글자 대문자 변환
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // ==================== Getters ====================

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getSkills() {
        return new ArrayList<>(skills);
    }

    public List<String> getActions() {
        return new ArrayList<>(actions);
    }

    public String getRarity() {
        return rarity;
    }

    public List<ItemStack> getStartingItems() {
        List<ItemStack> clonedItems = new ArrayList<>();
        for (ItemStack item : startingItems) {
            clonedItems.add(item.clone());
        }
        return clonedItems;
    }

    public ItemStack getIcon() {
        return icon.clone();
    }

    @Override
    public String toString() {
        return "Job{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", rarity='" + rarity + '\'' +
                '}';
    }
}