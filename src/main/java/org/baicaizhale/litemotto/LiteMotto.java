package org.baicaizhale.litemotto;

import org.baicaizhale.litemotto.Metrics.Metrics;
import org.baicaizhale.litemotto.api.LiteMottoAPI;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        saveDefaultConfig();

        // 注册命令和Tab补全
        this.getCommand("litemotto").setExecutor(this);
        this.getCommand("litemotto").setTabCompleter(new LiteMottoTabCompleter());

        // 初始化 bStats
        int pluginId = 25873; 
        Metrics metrics = new Metrics(this, pluginId);
        DebugManager.sendDebugMessage("&aLiteMotto 插件已启用！");

        // 启动配置文件监听器
        configWatcher = new ConfigWatcher(this, new java.io.File(getDataFolder(), "config.yml"));
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
                            
                            // 输出详细调试信息到控制台
                            DebugManager.sendDebugMessage("&7========== LiteMotto 调试信息 ==========");
                            DebugManager.sendDebugMessage("&f命令执行者: &e" + sender.getName());
                            DebugManager.sendDebugMessage("&a格言生成成功: &f" + motto);
                            DebugManager.sendDebugMessage("&f格言长度: &f" + motto.length() + " &f字符");
                            DebugManager.sendDebugMessage("&f最近格言列表大小: &f" + recentMottoManager.getRecentMottos().size());
                            DebugManager.sendDebugMessage("&f格言生成时间: &f" + new java.util.Date());
                            DebugManager.sendDebugMessage("&7======================================");

                            // 向所有处于调试模式的玩家发送详细调试信息
                            DebugManager.sendDebugMessage("&a格言生成成功");
                            DebugManager.sendDebugMessage("&f内容: &e" + motto);
                            DebugManager.sendDebugMessage("&f长度: &e" + motto.length() + " &f字符");
                            DebugManager.sendDebugMessage("&f已添加到最近格言列表，当前列表大小: &e" + 
                                recentMottoManager.getRecentMottos().size());
                        } else {
                            sender.sendMessage(PlayerJoinListener.colorize("&c获取格言失败，请稍后再试。"));
                            
                            // 输出详细调试信息到控制台
                            DebugManager.sendDebugMessage("&7========== LiteMotto Debug ==========");
                            DebugManager.sendDebugMessage("&f命令执行者: &e" + sender.getName());
                            DebugManager.sendDebugMessage("&c格言生成状态: 失败");
                            DebugManager.sendDebugMessage("&f可能原因: &e网络问题或API响应错误");
                            DebugManager.sendDebugMessage("&f尝试时间: &e" + new java.util.Date());
                            DebugManager.sendDebugMessage("&7======================================");

                            // 向所有处于调试模式的玩家发送详细调试信息
                            DebugManager.sendDebugMessage("&c格言生成失败");
                            DebugManager.sendDebugMessage("&f可能原因: &e网络问题或API响应错误");
                            DebugManager.sendDebugMessage("&f请检查控制台获取更多错误信息");
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