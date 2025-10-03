package org.baicaizhale.litemotto;

import org.baicaizhale.litemotto.Metrics.Metrics; // 添加 bStats 导入
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LiteMotto extends JavaPlugin {
    private static LiteMotto instance;
    private static RecentMottoManager recentMottoManager;

    @Override
    public void onEnable() {
        instance = this;
        recentMottoManager = new RecentMottoManager(10); // 保存最近10条
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        saveDefaultConfig();

        // 注册命令和Tab补全
        this.getCommand("litemotto").setExecutor(this);
        this.getCommand("litemotto").setTabCompleter(new LiteMottoTabCompleter());

        // 初始化 bStats
        int pluginId = 25873; // bStats 插件 ID
        Metrics metrics = new Metrics(this, pluginId);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("litemotto")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("gen")) {
                    // 异步获取格言
                    getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        String motto = PlayerJoinListener.fetchMottoFromAI();
                        if (motto != null) {
                            String prefix = getConfig().getString("prefix", "&6今日格言: &f");
                            sender.sendMessage(PlayerJoinListener.colorize(prefix + motto));
                            recentMottoManager.addMotto(motto); // 保存格言
                            
                            // 如果有调试模式玩家，输出调试信息
                            if (DebugManager.hasDebugPlayers()) {
                                getLogger().info("[Debug] 生成格言: " + motto);
                            }
                        } else {
                            sender.sendMessage(PlayerJoinListener.colorize("&c获取格言失败，请稍后再试。"));
                            
                            // 如果有调试模式玩家，输出调试信息
                            if (DebugManager.hasDebugPlayers()) {
                                getLogger().info("[Debug] 格言生成失败");
                            }
                        }
                    });
                    return true;
                } else if (args[0].equalsIgnoreCase("debug")) {
                    // 切换调试模式
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(PlayerJoinListener.colorize("&c只有玩家可以切换调试模式。"));
                        return true;
                    }
                    
                    Player player = (Player) sender;
                    boolean isDebugMode = DebugManager.toggleDebugMode(player);
                    if (isDebugMode) {
                        player.sendMessage(PlayerJoinListener.colorize("&a已开启调试模式，你将收到简短的调试信息。"));
                        getLogger().info("[Debug] 玩家 " + player.getName() + " 开启了调试模式");
                    } else {
                        player.sendMessage(PlayerJoinListener.colorize("&c已关闭调试模式。"));
                        getLogger().info("[Debug] 玩家 " + player.getName() + " 关闭了调试模式");
                    }
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static LiteMotto getInstance() {
        return instance;
    }

    public static RecentMottoManager getRecentMottoManager() {
        return recentMottoManager;
    }
}
