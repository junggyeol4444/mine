package com.yourname.jobplugin.util;

import org.bukkit.entity.Player;

public class ActionBarUtil {
    public static void send(Player player, String msg) {
        player.sendActionBar(msg);  // Paper 1.21 이상 표준 API
    }
}