package org.blog.minecraftJobPlugin.econ;

import org.blog.minecraftJobPlugin.JobPlugin;
import org.blog.minecraftJobPlugin.manager.JobManager;  // job → manager로 수정
import org.blog.minecraftJobPlugin.util.PluginDataUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * EconomyManager — Vault 연동(가능 시 reflection 사용) + 내부 캐시 + 비동기 저장
 */
public class EconomyManager {

    private final JobPlugin plugin;
    private final JobManager jobManager;
    private final PluginDataUtil dataUtil;
    private final Object vaultEcon;

    private final Map<UUID, Integer> balances = new ConcurrentHashMap<>();

    public EconomyManager(JobPlugin plugin, JobManager jobManager, Object vaultEcon) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.vaultEcon = vaultEcon;
        this.dataUtil = new PluginDataUtil(plugin);
        loadBalances();
    }

    private boolean hasVault() {
        return vaultEcon != null;
    }

    private double vaultGetBalance(Player player) {
        try {
            Method m = vaultEcon.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            Object res = m.invoke(vaultEcon, player);
            if (res instanceof Number) return ((Number) res).doubleValue();
        } catch (Exception ignored) {}
        return 0.0;
    }

    private boolean vaultHas(Player player, double amount) {
        try {
            Method m = vaultEcon.getClass().getMethod("has", org.bukkit.OfflinePlayer.class, double.class);
            Object res = m.invoke(vaultEcon, player, amount);
            if (res instanceof Boolean) return (Boolean) res;
        } catch (Exception ignored) {}
        return false;
    }

    private void vaultDeposit(Player player, double amount) {
        try {
            Method m = vaultEcon.getClass().getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
            m.invoke(vaultEcon, player, amount);
        } catch (Exception ignored) {}
    }

    private void vaultWithdraw(Player player, double amount) {
        try {
            Method m = vaultEcon.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
            m.invoke(vaultEcon, player, amount);
        } catch (Exception ignored) {}
    }

    public double getBalanceDouble(Player player) {
        if (hasVault()) return vaultGetBalance(player);
        return getBalance(player);
    }

    public int getBalance(Player player) {
        return balances.getOrDefault(player.getUniqueId(),
                dataUtil.loadPlayerConfig(player.getUniqueId()).getInt("balance", plugin.getConfig().getInt("economy.starting_balance", 0)));
    }

    public void addMoney(Player player, int amount) {
        if (amount <= 0) return;
        UUID id = player.getUniqueId();
        if (hasVault()) {
            vaultDeposit(player, amount);
        } else {
            balances.put(id, getBalance(player) + amount);
            dataUtil.savePlayerConfigAsync(id, buildBalanceCfg(id));
        }
        player.sendMessage("§a돈을 획득했습니다: +" + amount + plugin.getConfig().getString("economy.currency_symbol", "원") +
                " (현재: " + getBalance(player) + plugin.getConfig().getString("economy.currency_symbol", "원") + ")");
    }

    public boolean takeMoney(Player player, int amount) {
        if (amount <= 0) return true;
        UUID id = player.getUniqueId();
        if (hasVault()) {
            if (!vaultHas(player, amount)) return false;
            vaultWithdraw(player, amount);
            player.sendMessage("§c" + amount + plugin.getConfig().getString("economy.currency_symbol", "원") + " 사용되었습니다.");
            return true;
        } else {
            int cur = getBalance(player);
            if (cur < amount) return false;
            balances.put(id, cur - amount);
            dataUtil.savePlayerConfigAsync(id, buildBalanceCfg(id));
            player.sendMessage("§c" + amount + plugin.getConfig().getString("economy.currency_symbol", "원") + " 사용되었습니다.");
            return true;
        }
    }

    private YamlConfiguration buildBalanceCfg(UUID uuid) {
        YamlConfiguration cfg = dataUtil.loadPlayerConfig(uuid);
        cfg.set("balance", balances.getOrDefault(uuid, plugin.getConfig().getInt("economy.starting_balance", 0)));
        return cfg;
    }

    private void loadBalances() {
        java.io.File playerDir = new java.io.File(plugin.getDataFolder(), "data/player");
        if (!playerDir.exists()) return;
        java.io.File[] files = playerDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (java.io.File f : files) {
            try {
                UUID uuid = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                int bal = cfg.getInt("balance", plugin.getConfig().getInt("economy.starting_balance", 0));
                balances.put(uuid, bal);
            } catch (Exception ignored) {}
        }
    }

    public void saveAllAsync() {
        for (UUID uuid : balances.keySet()) {
            YamlConfiguration cfg = buildBalanceCfg(uuid);
            dataUtil.savePlayerConfigAsync(uuid, cfg);
        }
    }

    public void saveAll() {
        for (UUID uuid : balances.keySet()) {
            YamlConfiguration cfg = buildBalanceCfg(uuid);
            dataUtil.savePlayerConfigSync(uuid, cfg);
        }
    }
}