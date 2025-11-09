package org.baicaizhale.litemotto;


import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 管理插件的调试模式
 * 跟踪哪些玩家处于调试模式
 */
public class DebugManager {
    private static final Set<UUID> debugPlayers = new HashSet<>();
    
    /**
     * 切换玩家的调试模式状态
     * 
     * @param player 要切换调试模式的玩家
     * @return 切换后的调试模式状态
     */
    public static boolean toggleDebugMode(Player player) {
        UUID playerId = player.getUniqueId();
        if (debugPlayers.contains(playerId)) {
            debugPlayers.remove(playerId);
            return false;
        } else {
            debugPlayers.add(playerId);
            return true;
        }
    }
    
    /**
     * 检查玩家是否处于调试模式
     * 
     * @param player 要检查的玩家
     * @return 如果玩家处于调试模式则返回true
     */
    public static boolean isInDebugMode(Player player) {
        return player != null && debugPlayers.contains(player.getUniqueId());
    }
    
    /**
     * 检查是否有任何玩家处于调试模式
     * 
     * @return 如果有任何玩家处于调试模式则返回true
     */
    public static boolean hasDebugPlayers() {
        return !debugPlayers.isEmpty();
    }
    
    /**
     * 向处于调试模式的玩家发送调试信息
     * 
     * @param message 要发送的调试信息
     */
    public static void sendDebugMessage(String message) {
        // 向控制台输出调试信息，移除Minecraft颜色代码
        String formattedConsoleMessage = PlayerJoinListener.colorize("&7[LiteMotto Debug] &f" + message);
        LiteMotto.getInstance().getLogger().info(PlayerJoinListener.toAnsiColor(formattedConsoleMessage));

        for (UUID playerId : debugPlayers) {
            Player player = LiteMotto.getInstance().getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(PlayerJoinListener.colorize("&7[LiteMotto Debug] &f" + message));
            }
        }
    }
}