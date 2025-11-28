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
 * ShopGUI - 구매 + 판매 (여러 개 동시 판매 지원)
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
        while (size < count + 1) size += 9;
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

        ItemStack sellBtn = new ItemStack(Material.EMERALD);
        ItemMeta smeta = sellBtn.getItemMeta();
        smeta.setDisplayName(ChatColor.GOLD + "판매하기");
        smeta.setLore(Arrays.asList(ChatColor.GRAY + "우클릭: 판매 창 열기", ChatColor.GRAY + "여러 아이템을 한번에 판매할 수 있습니다"));
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

    /**
     * 판매 창 열기 - 27칸 (3줄)
     * 상단 2줄(18칸): 아이템 배치
     * 하단 1줄: 취소(0), 안내(4), 판매확인(8)
     */
    public void openSell() {
        Inventory inv = Bukkit.createInventory(null, 27, SELL_TITLE_PREFIX + job);
        
        // 하단 컨트롤 버튼들
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cm = cancel.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "취소");
        cm.setLore(Arrays.asList(ChatColor.GRAY + "판매를 취소합니다"));
        cancel.setItemMeta(cm);
        inv.setItem(18, cancel);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "판매 안내");
        im.setLore(Arrays.asList(
            ChatColor.GRAY + "상단 18칸에 판매할 아이템을 놓으세요",
            ChatColor.GRAY + "우측 하단 초록 버튼으로 일괄 판매"
        ));
        info.setItemMeta(im);
        inv.setItem(22, info);

        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta cfm = confirm.getItemMeta();
        cfm.setDisplayName(ChatColor.GREEN + "§l판매 확인");
        cfm.setLore(Arrays.asList(
            ChatColor.GRAY + "상단에 놓인 모든 아이템을",
            ChatColor.GRAY + "일괄 판매합니다"
        ));
        confirm.setItemMeta(cfm);
        inv.setItem(26, confirm);

        player.openInventory(inv);
    }

    public static boolean isShopInventory(String title) {
        return title != null && title.startsWith(SHOP_TITLE_PREFIX);
    }

    public static boolean isSellInventory(String title) {
        return title != null && title.startsWith(SELL_TITLE_PREFIX);
    }

    /**
     * 판매 창 클릭 처리 (여러 아이템 동시 판매)
     */
    public static boolean handleSellClick(org.bukkit.event.inventory.InventoryClickEvent e, JobPlugin plugin) {
        if (e.getWhoClicked() == null) return false;
        String title = e.getView().getTitle();
        if (!isSellInventory(title)) return false;
        
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        int slot = e.getRawSlot();

        // 하단 버튼들만 취소 (상단은 아이템 배치 가능)
        if (slot >= 18 && slot < 27) {
            e.setCancelled(true);
            
            // 취소 버튼 (슬롯 18)
            if (slot == 18) {
                p.closeInventory();
                p.sendMessage("§e판매가 취소되었습니다.");
                return true;
            }
            
            // 판매 확인 버튼 (슬롯 26)
            if (slot == 26) {
                int totalMoney = 0;
                List<ItemStack> soldItems = new ArrayList<>();
                
                // 상단 18칸 체크 (0~17)
                for (int i = 0; i < 18; i++) {
                    ItemStack item = inv.getItem(i);
                    if (item == null || item.getType() == Material.AIR) continue;
                    
                    Material mat = item.getType();
                    int amount = item.getAmount();
                    
                    // 판매 가격 계산
                    int unitPrice = getSellPrice(plugin, title.replace(SELL_TITLE_PREFIX, ""), mat);
                    int itemTotal = unitPrice * amount;
                    
                    totalMoney += itemTotal;
                    soldItems.add(item.clone());
                    
                    // 인벤토리에서 제거
                    inv.setItem(i, null);
                }
                
                if (soldItems.isEmpty()) {
                    p.sendMessage("§c판매할 아이템이 없습니다.");
                    return true;
                }
                
                // 돈 지급
                plugin.getEconomyManager().addMoney(p, totalMoney);
                
                // 판매 내역 출력
                p.sendMessage("§a━━━━━━ 판매 완료 ━━━━━━");
                for (ItemStack item : soldItems) {
                    int unitPrice = getSellPrice(plugin, title.replace(SELL_TITLE_PREFIX, ""), item.getType());
                    int total = unitPrice * item.getAmount();
                    p.sendMessage("§f- " + item.getType().name() + " §7x" + item.getAmount() + 
                                " §e→ " + total + plugin.getConfig().getString("economy.currency_symbol", "원"));
                }
                p.sendMessage("§a총 수익: §6" + totalMoney + plugin.getConfig().getString("economy.currency_symbol", "원"));
                p.sendMessage("§a━━━━━━━━━━━━━━━━━━");
                
                p.closeInventory();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 아이템 판매 가격 조회 (shops.yml 기반)
     */
    private static int getSellPrice(JobPlugin plugin, String job, Material mat) {
        PluginDataUtil dataUtil = new PluginDataUtil(plugin);
        YamlConfiguration shops = dataUtil.loadGlobal("shops");
        int price = 0;
        
        if (shops != null) {
            // 직업별 판매 가격 우선
            if (job != null && !job.isBlank()) {
                price = shops.getInt("shops." + job + ".sell." + mat.name(), 0);
            }
            // 전역 판매 가격
            if (price <= 0) {
                price = shops.getInt("shops.global.sell." + mat.name(), 0);
            }
        }
        
        // 기본 판매 가격
        if (price <= 0) {
            price = plugin.getConfig().getInt("equipment.default_sell_price", 10);
        }
        
        return price;
    }
}