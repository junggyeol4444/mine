package org.blog.minecraftJobPlugin.gui;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.econ.EconomyManager;
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * ShopGUI - 구매(기존) + 판매 통합
 *
 * 사용법:
 * - open() : 기존 상점 열기 (구매)
 * - 상점 내부의 '판매하기' 버튼을 누르면 판매 전용 창이 열림
 * - 판매 창에 아이템을 넣고 '판매 확인' 버튼을 누르면 판매 처리
 */
public class ShopGUI {

    private final JobPlugin plugin;
    private final Player player;
    private final String job;
    private final PluginDataUtil dataUtil;
    private final EconomyManager econ;

    public static final String SHOP_TITLE_PREFIX = "직업 상점: ";
    public static final String SELL_TITLE_PREFIX = "판매 창: ";

    public ShopGUI(JobPlugin plugin, Player player, String job) {
        this.plugin = plugin;
        this.player = player;
        this.job = job;
        this.dataUtil = new PluginDataUtil(plugin);
        this.econ = plugin.getEconomyManager();
    }

    public void open() {
        YamlConfiguration shops = dataUtil.loadGlobal("shops");
        List<?> items = shops.getList("shops." + job + ".items", Collections.emptyList());
        int size = 9;
        int count = items.size();
        while (size < count + 1) size += 9; // +1 for 판매 버튼
        Inventory inv = Bukkit.createInventory(null, size, SHOP_TITLE_PREFIX + job);
        int i = 0;
        for (Object o : items) {
            if (!(o instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) o;
            String name = String.valueOf(map.getOrDefault("name", "아이템"));
            int price = ((Number)map.getOrDefault("price", 0)).intValue();
            String matStr = String.valueOf(map.getOrDefault("material", "STONE"));
            int amount = ((Number)map.getOrDefault("amount", 1)).intValue();
            Material mat = Material.matchMaterial(matStr);
            if (mat == null) mat = Material.STONE;
            ItemStack item = new ItemStack(mat, amount);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + name);
            meta.setLore(Arrays.asList(ChatColor.YELLOW + "가격: " + price + plugin.getConfig().getString("economy.currency_symbol", "원"),
                    ChatColor.GRAY + "좌클릭: 구매"));
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }

        // 판매 버튼 (마지막 슬롯에 배치)
        ItemStack sellBtn = new ItemStack(Material.EMERALD);
        ItemMeta smeta = sellBtn.getItemMeta();
        smeta.setDisplayName(ChatColor.GOLD + "판매하기");
        smeta.setLore(Arrays.asList(ChatColor.GRAY + "우클릭: 판매 창 열기", ChatColor.GRAY + "손에 든 아이템을 판매하려면 클릭"));
        sellBtn.setItemMeta(smeta);
        inv.setItem(size - 1, sellBtn);

        player.openInventory(inv);
    }

    public boolean handlePurchase(ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return false;
        String display = clicked.getItemMeta().getDisplayName();
        if (display == null) return false;
        List<String> lore = clicked.getItemMeta().getLore();
        int price = 0;
        if (lore != null) {
            for (String line : lore) {
                if (line.contains("가격:")) {
                    String s = ChatColor.stripColor(line).replace("가격:", "").replace(plugin.getConfig().getString("economy.currency_symbol", "원"), "").trim();
                    try { price = Integer.parseInt(s); } catch (Exception ignored) {}
                }
            }
        }
        if (price <= 0) {
            player.sendMessage("§c해당 아이템은 구매 불가합니다.");
            return false;
        }
        if (!econ.takeMoney(player, price)) {
            player.sendMessage("§c잔액이 부족합니다. 가격: " + price + plugin.getConfig().getString("economy.currency_symbol", "원"));
            return false;
        }
        player.getInventory().addItem(clicked);
        player.sendMessage("§a구매 완료: " + ChatColor.stripColor(display) + " - " + price + plugin.getConfig().getString("economy.currency_symbol", "원"));
        return true;
    }

    public void openSell() {
        Inventory inv = Bukkit.createInventory(null, 9, SELL_TITLE_PREFIX + job);
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cm = cancel.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "취소");
        cm.setLore(Arrays.asList(ChatColor.GRAY + "취소하려면 클릭"));
        cancel.setItemMeta(cm);
        inv.setItem(0, cancel);

        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = placeholder.getItemMeta();
        pm.setDisplayName(ChatColor.GRAY + "여기에 아이템을 넣으세요");
        placeholder.setItemMeta(pm);
        inv.setItem(4, placeholder);

        ItemStack confirm = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta im = confirm.getItemMeta();
        im.setDisplayName(ChatColor.GREEN + "판매 확인");
        im.setLore(Arrays.asList(ChatColor.GRAY + "여기에 넣은 아이템을 판매합니다"));
        confirm.setItemMeta(im);
        inv.setItem(8, confirm);

        player.openInventory(inv);
    }

    public static boolean isShopInventory(String title) {
        return title != null && title.startsWith(SHOP_TITLE_PREFIX);
    }

    public static boolean isSellInventory(String title) {
        return title != null && title.startsWith(SELL_TITLE_PREFIX);
    }

    public static boolean handleSellClick(org.bukkit.event.inventory.InventoryClickEvent e, JobPlugin plugin) {
        if (e.getWhoClicked() == null) return false;
        String title = e.getView().getTitle();
        if (!isSellInventory(title)) return false;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        int slot = e.getRawSlot();

        if (slot == 0) {
            p.closeInventory();
            p.sendMessage("§e판매 취소");
            return true;
        }
        if (slot == 8) {
            ItemStack target = inv.getItem(4);
            if (target == null || target.getType() == Material.AIR || !target.hasItemMeta()) {
                p.sendMessage("§c판매할 아이템을 중앙 슬롯에 넣어주세요.");
                return true;
            }
            Material mat = target.getType();
            PluginDataUtil dataUtil = new PluginDataUtil(plugin);
            YamlConfiguration shops = dataUtil.loadGlobal("shops");
            int price = 0;
            String job = title.replace(SELL_TITLE_PREFIX, "");
            if (shops != null) {
                if (job != null && !job.isBlank()) price = shops.getInt("shops." + job + ".sell." + mat.name(), 0);
                if (price <= 0) price = shops.getInt("shops.global.sell." + mat.name(), 0);
            }
            if (price <= 0) price = plugin.getConfig().getInt("equipment.default_sell_price", 10);
            int amount = target.getAmount();
            int total = price * amount;
            inv.setItem(4, null);
            p.closeInventory();
            plugin.getEconomyManager().addMoney(p, total);
            p.sendMessage("§a판매 완료: " + mat.name() + " x" + amount + " → " + total + plugin.getConfig().getString("economy.currency_symbol", "원"));
            return true;
        }

        return true;
    }
}