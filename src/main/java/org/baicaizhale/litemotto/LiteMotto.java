package org.baicaizhale.litemotto;

import org.baicaizhale.litemotto.Metrics.Metrics; // 添加 bStats 导入
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LiteMotto extends JavaPlugin {
    private static LiteMotto instance;
    private static RecentMottoManager recentMottoManager;
    private ConfigWatcher configWatcher;

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

        // 启动配置文件监听器
        configWatcher = new ConfigWatcher(this, new java.io.File(getDataFolder(), "config.yml"));
        getServer().getScheduler().runTaskAsynchronously(this, configWatcher);
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
                            
                            // 输出详细调试信息到控制台
                            getLogger().info("========== LiteMotto 调试信息 ==========");
                            getLogger().info("[LiteMotto Debug] 命令执行者: " + sender.getName());
                            getLogger().info("[LiteMotto Debug] 格言生成成功: " + motto);
                            getLogger().info("[LiteMotto Debug] 格言长度: " + motto.length() + " 字符");
                            getLogger().info("[LiteMotto Debug] 最近格言列表大小: " + recentMottoManager.getRecentMottos().size());
                            getLogger().info("[LiteMotto Debug] 格言生成时间: " + new java.util.Date());
                            getLogger().info("======================================");

                            // 如果命令执行者是玩家且处于调试模式，发送详细调试信息
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                if (DebugManager.isInDebugMode(player)) {
                                    player.sendMessage(PlayerJoinListener.colorize("&7[Debug] &a格言生成成功"));
                                    player.sendMessage(PlayerJoinListener.colorize("&7[Debug] &f内容: &e" + motto));
                                    player.sendMessage(PlayerJoinListener.colorize("&7[Debug] &f长度: &e" + motto.length() + " &f字符"));
                                    player.sendMessage(PlayerJoinListener.colorize("&7[Debug] &f已添加到最近格言列表，当前列表大小: &e" + 
                                        recentMottoManager.getRecentMottos().size()));
                                }
                            }
                        } else {
                            sender.sendMessage(PlayerJoinListener.colorize("&c获取格言失败，请稍后再试。"));
                            
                            // 输出详细调试信息到控制台
                            getLogger().info("========== LiteMotto Debug ==========");
                            getLogger().info("[LiteMotto Debug] 命令执行者: " + sender.getName());
                            getLogger().info("[LiteMotto Debug] 格言生成状态: 失败");
                            getLogger().info("[LiteMotto Debug] 可能原因: 网络问题或API响应错误");
                            getLogger().info("[LiteMotto Debug] 尝试时间: " + new java.util.Date());
                            getLogger().info("======================================");

                            // 如果命令执行者是玩家且处于调试模式，发送详细调试信息
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                if (DebugManager.isInDebugMode(player)) {
                                    player.sendMessage(PlayerJoinListener.colorize("&7[Debug] &c格言生成失败"));
                                    player.sendMessage(PlayerJoinListener.colorize("&7[Debug] &f可能原因: &e网络问题或API响应错误"));
                                    player.sendMessage(PlayerJoinListener.colorize("&7[Debug] &f请检查控制台获取更多错误信息"));
                                }
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
                        getLogger().info("[LiteMotto Debug] 玩家 " + player.getName() + " 开启了调试模式");
                    } else {
                        player.sendMessage(PlayerJoinListener.colorize("&c已关闭调试模式。"));
                        getLogger().info("[LiteMotto Debug] 玩家 " + player.getName() + " 关闭了调试模式");
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
                    getLogger().info("[LiteMotto Debug] 插件配置已由 " + sender.getName() + " 重载。");
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
        instance = null;
    }

    public static LiteMotto getInstance() {
        return instance;
    }

    public static RecentMottoManager getRecentMottoManager() {
        return recentMottoManager;
    }
}
