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
        String formattedConsoleMessage = PlayerJoinListener.colorize("&7Debug &f>> " + message);
        LiteMotto.getInstance().getLogger().info(PlayerJoinListener.toAnsiColor(formattedConsoleMessage));

        for (UUID playerId : debugPlayers) {
            Player player = LiteMotto.getInstance().getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(PlayerJoinListener.colorize("&7Debug &f>> " + message));
            }
        }
    }

    /**
     * 发送格言生成调试信息
     * @param sender 执行命令的发送者
     * @param motto 生成的格言 (如果成功)
     * @param success 是否生成成功
     */
    public static void sendGenerationDebug(org.bukkit.command.CommandSender sender, String motto, boolean success) {
        if (success) {
            // sendDebugMessage("&7========== LiteMotto 调试信息 ==========");
            sendDebugMessage("&a格言生成成功");
            sendDebugMessage("&f命令执行者: &e" + sender.getName());
            sendDebugMessage("&f内容: &e" + motto);
            sendDebugMessage("&f长度: &e" + motto.length() + " &f字符");
            sendDebugMessage("&f最近格言列表大小: &e" + LiteMotto.getRecentMottoManager().getRecentMottos().size());
            sendDebugMessage("&f格言生成时间: &f" + new java.util.Date());
            // sendDebugMessage("&7======================================");
        } else {
            sendDebugMessage("&7========== LiteMotto Report ==========");
            sendDebugMessage("&c格言生成状态: 失败");
            sendDebugMessage("&f命令执行者: &e" + sender.getName());
            sendDebugMessage("&f可能原因: &e网络问题或API响应错误");
            sendDebugMessage("&f尝试时间: &e" + new java.util.Date());
            sendDebugMessage("&7======================================");
        }
    }

    /**
     * 发送玩家加入时的格言生成调试信息
     * @param player 加入的玩家
     * @param motto 生成的格言 (如果成功)
     * @param success 是否生成成功
     */
    public static void sendPlayerJoinDebug(Player player, String motto, boolean success) {
        if (success) {
            // sendDebugMessage("&7========== LiteMotto 调试信息 ==========");
            sendDebugMessage("&7玩家 &f" + player.getName() + " &7加入服务器");
            sendDebugMessage("&7UUID: &f" + player.getUniqueId());
            sendDebugMessage("&a格言生成成功: &f" + motto);
            sendDebugMessage("&f内容: &e" + motto);
            sendDebugMessage("&7格言长度: &f" + motto.length() + " &7字符");
            sendDebugMessage("&7最近格言列表大小: &f" + LiteMotto.getRecentMottoManager().getRecentMottos().size());
            sendDebugMessage("&7格言生成时间: &f" + new java.util.Date());
            // sendDebugMessage("&7======================================");
        } else {
            sendDebugMessage("&7=========== LiteMotto Report ==========");
            sendDebugMessage("&7玩家 &f" + player.getName() + " &7加入服务器");
            sendDebugMessage("&7UUID: &f" + player.getUniqueId());
            sendDebugMessage("&c格言生成状态: 失败");
            sendDebugMessage("&7可能原因: &e网络问题或API响应错误");
            sendDebugMessage("&7尝试时间: &f" + new java.util.Date());
            sendDebugMessage("&7======================================");
        }
    }
}