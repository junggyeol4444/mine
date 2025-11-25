package com.yourname.jobplugin.gui;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.econ.EconomyManager;
import com.yourname.jobplugin.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.List;

public class ShopGUI {

    private final Player player;
    private final String jobType;
    private final JobPlugin plugin;

    public ShopGUI(JobPlugin plugin, Player player, String jobType) {
        this.plugin = plugin;
        this.player = player;
        this.jobType = jobType;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(player, 27, jobType + " 전용 상점");

        ConfigurationSection jobShopSec = ConfigUtil.getConfig("shops").getConfigurationSection("shops." + jobType);
        if (jobShopSec != null) {
            List<?> items = jobShopSec.getList("items");
            for (int i = 0; i < items.size() && i < 27; i++) {
                Object itemObj = items.get(i);
                if (itemObj instanceof ConfigurationSection config) {
                    ItemStack stack = toItemStack(config);
                    inv.setItem(i, stack);
                }
            }
        }
        player.openInventory(inv);

        // 이벤트 리스너에서 클릭 시 구매/지불/잔액체크 후 지급 구현!
    }

    // 아이템 yaml → 인벤 아이템 변환
    private ItemStack toItemStack(ConfigurationSection config) {
        Material mat = Material.matchMaterial(config.getString("material", "PAPER"));
        ItemStack item = new ItemStack(mat != null ? mat : Material.PAPER, config.getInt("amount", 1));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + config.getString("name"));
        List<String> lore = new ArrayList<>();
        lore.add("§6가격: " + config.getInt("price") + "원");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}