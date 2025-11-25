package com.yourname.jobplugin.econ;

import com.yourname.jobplugin.JobPlugin;
import com.yourname.jobplugin.job.JobManager;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final JobPlugin plugin;
    private final JobManager jobManager;

    // 플레이어 잔액
    private final Map<UUID, Integer> balances = new HashMap<>();

    public EconomyManager(JobPlugin plugin, JobManager jobManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        loadBalances();
    }

    public int getBalance(Player player) {
        return balances.getOrDefault(player.getUniqueId(), 0);
    }

    public void addMoney(Player player, int amount) {
        balances.put(player.getUniqueId(), getBalance(player) + amount);
        player.sendMessage("§a돈을 획득했습니다: +" + amount + "원 (현재: " + getBalance(player) + "원)");
    }

    public boolean takeMoney(Player player, int amount) {
        int cur = getBalance(player);
        if (cur < amount) return false;
        balances.put(player.getUniqueId(), cur - amount);
        player.sendMessage("§c" + amount + "원이 사용되었습니다. (현재: " + getBalance(player) + "원)");
        return true;
    }

    // 직업별 상점 할인
    public int getDiscount(Player player, int basePrice) {
        // 등급, 조합, 특성에 따른 할인율 적용 (예시 10%)
        return (int)(basePrice * 0.9);
    }

    // 플레이어별 수익/판매 연동
    public void rewardForJobAction(Player player, String job, String action) {
        // 행동별 수익(밸런스), 직업별 수입 등등
        int reward = 50; // 예시
        addMoney(player, reward);
    }

    // 저장/로드
    private void loadBalances() {
        // TODO: 데이터파일(또는 DB)에서 플레이어별 잔액 로드
    }
    public void saveAll() {
        // TODO: balances 저장
    }
}