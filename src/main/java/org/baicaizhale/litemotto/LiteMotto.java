package org.baicaizhale.litemotto;

import org.baicaizhale.litemotto.Metrics.Metrics;
import org.baicaizhale.litemotto.api.LiteMottoAPI;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
        updateConfig();

        // 注册命令和Tab补全
        this.getCommand("litemotto").setExecutor(this);
        this.getCommand("litemotto").setTabCompleter(new LiteMottoTabCompleter());

        // 初始化 bStats
        int pluginId = 25873; 
        new Metrics(this, pluginId);
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

    /**
     * 更新配置文件逻辑
     * 1. 获取当前版本号
     * 2. 比较版本号，如果不一样或不存在则执行更新
     * 3. 读取 jar 包内的资源文件写入 temp.yml
     * 4. 合并旧配置到 temp.yml
     * 5. 备份旧 config.yml 并替换
     */
    private void updateConfig() {
        String currentVersion = getDescription().getVersion();
        File configFile = new File(getDataFolder(), "config.yml");

        // 如果文件不存在，直接保存并结束
        if (!configFile.exists()) {
            saveDefaultConfig();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String configVersion = config.getString("version");

        // 如果版本号不一样或者不存在
        if (configVersion == null || !configVersion.equals(currentVersion)) {
            getLogger().info("检测到版本更新，正在自动迁移配置...");
            
            File tempFile = new File(getDataFolder(), "temp.yml");
            File oldConfigFile = new File(getDataFolder(), "config.yml.old");

            try {
                // 3. 读取 jar 包内的资源文件，写入到 temp.yml
                try (InputStream in = getResource("config.yml")) {
                    if (in != null) {
                        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        getLogger().severe("无法从 JAR 包中读取默认 config.yml！");
                        return;
                    }
                }

                // 4. 读取当前 config.yml 的配置，替换 temp.yml 中的值
                FileConfiguration tempConfig = YamlConfiguration.loadConfiguration(tempFile);
                for (String key : config.getKeys(true)) {
                    // 只迁移 temp.yml 中已有的键，且不是 version 键
                    if (tempConfig.contains(key) && !key.equals("version")) {
                        // 只迁移具体的配置值（非 Section）
                        if (!config.isConfigurationSection(key)) {
                            tempConfig.set(key, config.get(key));
                        }
                    }
                }
                
                // 确保新版本号被写入
                tempConfig.set("version", currentVersion);
                tempConfig.save(tempFile);

                // 5. 将当前的 config.yml 重命名为 config.yml.old，将 temp 重命名为 config.yml
                if (oldConfigFile.exists()) {
                    oldConfigFile.delete();
                }
                
                if (configFile.renameTo(oldConfigFile)) {
                    if (tempFile.renameTo(configFile)) {
                        getLogger().info("配置文件更新成功！旧配置已备份为 config.yml.old");
                    } else {
                        getLogger().severe("无法将 temp.yml 重命名为 config.yml！");
                    }
                } else {
                    getLogger().severe("无法备份旧的 config.yml！");
                }

            } catch (IOException e) {
                getLogger().severe("更新配置文件时发生错误: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 清理临时文件
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
            
            // 重新加载配置到内存
            reloadConfig();
        }
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