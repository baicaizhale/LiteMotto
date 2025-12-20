package org.baicaizhale.litemotto;


import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.baicaizhale.litemotto.api.LiteMottoAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否启用了管理员进服检查更新功能
        if (LiteMotto.getInstance().getConfig().getBoolean("update-check.on-admin-join", true)) {
            // 检查玩家是否有权限接收更新通知
            if (player.hasPermission("litemotto.update")) {
                // 使用更新检查器检查更新（仅向该玩家发送通知）
                UpdateChecker updateChecker = new UpdateChecker(LiteMotto.getInstance(), true);
                updateChecker.checkForUpdates();
            }
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(LiteMotto.getInstance(), () -> {
            String motto = fetchMottoFromAI();
            if (motto != null) {
                String prefix = LiteMotto.getInstance().getConfig().getString("prefix", "&6今日格言: &f");
                player.sendMessage(colorize(prefix + motto));
                LiteMotto.getRecentMottoManager().addMotto(motto); // 保存格言
                
                // 发送详细调试信息
                DebugManager.sendPlayerJoinDebug(player, motto, true);
            } else {
                player.sendMessage(colorize("&c获取格言失败，请稍后再试。"));
                
                // 发送详细调试信息
                DebugManager.sendPlayerJoinDebug(player, null, false);
            }
        });
    }

    /**
     * 从AI获取格言。
     * 该方法现在通过 LiteMottoAPI 获取当前配置的格言生成器，并调用其 fetchMotto 方法。
     *
     * @return 从AI获取的格言字符串，如果获取失败则返回 null。
     */
    public static String fetchMottoFromAI() {
        try {
            String prompt = LiteMotto.getInstance().getConfig().getString("prompt", "请生成一条简短的格言");

            // 拼接最近格言，提示AI不要重复
            StringBuilder avoidBuilder = new StringBuilder();
            for (String recent : LiteMotto.getRecentMottoManager().getRecentMottos()) {
                avoidBuilder.append("「").append(recent).append("」, ");
            }
            String avoidText = avoidBuilder.length() > 0
                    ? "请不要生成以下内容：" + avoidBuilder.toString() + "。"
                    : "";
            String finalPrompt = prompt + (avoidText.isEmpty() ? "" : "\n" + avoidText);

            LiteMottoAPI api = Bukkit.getServicesManager().getRegistration(LiteMottoAPI.class).getProvider();
            if (api == null) {
                Bukkit.getLogger().severe("LiteMotto API 未注册。请确保 LiteMotto 插件已正确加载。");
                return null;
            }
            String motto = api.fetchMottoWithPrompt(finalPrompt);
            return colorize(motto);
        } catch (Exception e) {
            Bukkit.getLogger().severe("获取格言失败: " + e.getMessage());
            return null;
        }
    }

    // 颜色代码转换方法：将 & 替换成 §
    public static String colorize(String message) {
        return message.replace("&", "§");
    }

    /**
     * 将带有Minecraft颜色代码的字符串转换为带有ANSI颜色代码的字符串，用于控制台输出。
     * @param message 带有Minecraft颜色代码的字符串。
     * @return 带有ANSI颜色代码的字符串。
     */
    public static String toAnsiColor(String message) {
        String coloredMessage = colorize(message); // 先将 & 转换为 §
        StringBuilder ansiMessage = new StringBuilder();
        for (int i = 0; i < coloredMessage.length(); i++) {
            char c = coloredMessage.charAt(i);
            if (c == '§' && i + 1 < coloredMessage.length()) {
                char colorCode = coloredMessage.charAt(i + 1);
                switch (colorCode) {
                    case '0': ansiMessage.append("\u001B[30m"); break; // Black
                    case '1': ansiMessage.append("\u001B[34m"); break; // Dark Blue
                    case '2': ansiMessage.append("\u001B[32m"); break; // Dark Green
                    case '3': ansiMessage.append("\u001B[36m"); break; // Dark Aqua
                    case '4': ansiMessage.append("\u001B[31m"); break; // Dark Red
                    case '5': ansiMessage.append("\u001B[35m"); break; // Dark Purple
                    case '6': ansiMessage.append("\u001B[33m"); break; // Gold
                    case '7': ansiMessage.append("\u001B[37m"); break; // Gray
                    case '8': ansiMessage.append("\u001B[90m"); break; // Dark Gray
                    case '9': ansiMessage.append("\u001B[94m"); break; // Blue
                    case 'a': ansiMessage.append("\u001B[92m"); break; // Green
                    case 'b': ansiMessage.append("\u001B[96m"); break; // Aqua
                    case 'c': ansiMessage.append("\u001B[91m"); break; // Red
                    case 'd': ansiMessage.append("\u001B[95m"); break; // Light Purple
                    case 'e': ansiMessage.append("\u001B[93m"); break; // Yellow
                    case 'f': ansiMessage.append("\u001B[97m"); break; // White
                    case 'r': ansiMessage.append("\u001B[0m"); break;  // Reset
                    default: ansiMessage.append(c); // If not a recognized color code, append as is
                }
                i++; // Skip the color code character
            } else {
                ansiMessage.append(c);
            }
        }
        ansiMessage.append("\u001B[0m"); // Reset color at the end
        return ansiMessage.toString();
    }
}