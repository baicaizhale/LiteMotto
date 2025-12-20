package org.baicaizhale.litemotto;

import org.baicaizhale.litemotto.Metrics.Metrics;
import org.baicaizhale.litemotto.api.LiteMottoAPI;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.io.File;

public class LiteMotto extends JavaPlugin {
    private static LiteMotto instance;
    private static RecentMottoManager recentMottoManager;
    private ConfigWatcher configWatcher;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        recentMottoManager = new RecentMottoManager(10); // 保存最近10条
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        
        // 自动更新配置文件
        new ConfigUpdater(this).updateConfig();

        // 注册命令和Tab补全
        this.getCommand("litemotto").setExecutor(this);
        this.getCommand("litemotto").setTabCompleter(new LiteMottoTabCompleter());

        // 初始化 bStats
        int pluginId = 25873; 
        new Metrics(this, pluginId);
        DebugManager.sendDebugMessage("&aLiteMotto 插件已启用！");

        // 启动配置文件监听器
        configWatcher = new ConfigWatcher(this, new File(getDataFolder(), "config.yml"));
        getServer().getScheduler().runTaskAsynchronously(this, configWatcher);
        
        // 初始化更新检查器
        updateChecker = new UpdateChecker(this, true, false);
        // 根据配置决定是否在启动时检查更新
        if (getConfig().getBoolean("update-check.on-startup", true)) {
            updateChecker.checkForUpdates();
        }
        
        // 注册API服务
        getServer().getServicesManager().register(LiteMottoAPI.class, new LiteMottoAPI(), this, org.bukkit.plugin.ServicePriority.Normal);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("litemotto")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("gen")) {
                    if (!sender.hasPermission("litemotto.gen")) {
                        sender.sendMessage(PlayerJoinListener.colorize("&c你没有权限执行此命令。"));
                        return true;
                    }
                    // 异步获取格言
                    getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        String motto = PlayerJoinListener.fetchMottoFromAI();
                        if (motto != null) {
                            String prefix = getConfig().getString("prefix", "&6今日格言: &f");
                            sender.sendMessage(PlayerJoinListener.colorize(prefix + motto));
                            recentMottoManager.addMotto(motto); // 保存格言
                            
                            // 发送详细调试信息
                            DebugManager.sendGenerationDebug(sender, motto, true);
                        } else {
                            sender.sendMessage(PlayerJoinListener.colorize(getConfig().getString("messages.motto-generation-failed", "&c获取格言失败，请稍后再试。")));
                            
                            // 发送详细调试信息
                            DebugManager.sendGenerationDebug(sender, null, false);
                        }
                    });
                    return true;
                } else if (args[0].equalsIgnoreCase("debug")) {
                    if (!sender.hasPermission("litemotto.debug")) {
                        sender.sendMessage(PlayerJoinListener.colorize("&c你没有权限执行此命令。"));
                        return true;
                    }
                    // 切换调试模式
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(PlayerJoinListener.colorize("&c只有玩家可以切换调试模式。"));
                        return true;
                    }
                    
                    Player player = (Player) sender;
                    boolean isDebugMode = DebugManager.toggleDebugMode(player);
                    if (isDebugMode) {
                        player.sendMessage(PlayerJoinListener.colorize("&a已开启调试模式，你将收到简短的调试信息。"));
                        DebugManager.sendDebugMessage("玩家 " + player.getName() + " 开启了调试模式");
                    } else {
                        player.sendMessage(PlayerJoinListener.colorize("&c已关闭调试模式。"));
                        DebugManager.sendDebugMessage("玩家 " + player.getName() + " 关闭了调试模式");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("reload")) {
                    // 重载插件配置
                    if (!sender.hasPermission("litemotto.reload")) {
                        sender.sendMessage(PlayerJoinListener.colorize("&c你没有权限执行此命令。"));
                        return true;
                    }
                    reloadConfig();
                    // 获取已注册的 LiteMottoAPI 实例并重新初始化其生成器
                    LiteMottoAPI api = getServer().getServicesManager().getRegistration(LiteMottoAPI.class).getProvider();
                    if (api != null) {
                        api.initMottoGenerator(); // 重新初始化生成器
                        getLogger().info("LiteMotto: API 生成器已根据新配置重新初始化。");
                    } else {
                        getLogger().severe("LiteMotto: 无法获取 LiteMottoAPI 实例，API 生成器未能重新初始化。");
                    }
                    sender.sendMessage(PlayerJoinListener.colorize("&aLiteMotto 配置已重载。"));
                    DebugManager.sendDebugMessage("&a插件配置已由 &f" + sender.getName() + " &a重载。");
                    return true;
                } else if (args[0].equalsIgnoreCase("update")) {
                    // 检查更新
                    if (!sender.hasPermission("litemotto.update")) {
                        sender.sendMessage(PlayerJoinListener.colorize("&c你没有权限执行此命令。"));
                        return true;
                    }
                    
                    sender.sendMessage(PlayerJoinListener.colorize("&6正在检查更新..."));
                    UpdateChecker manualUpdateChecker = new UpdateChecker(this, false, true);
                    // 如果是玩家执行的命令，确保通知该玩家更新信息
                    if (sender instanceof Player) {
                        manualUpdateChecker.checkForUpdates((Player) sender);
                    } else {
                        manualUpdateChecker.checkForUpdates();
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
        if (configWatcher != null) {
            configWatcher.stopWatching();
        }
        DebugManager.sendDebugMessage("&cLiteMotto 插件已禁用！");
        instance = null;
    }

    public static LiteMotto getInstance() {
        return instance;
    }

    public static RecentMottoManager getRecentMottoManager() {
        return recentMottoManager;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}