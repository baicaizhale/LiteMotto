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
            if (args.length > 0 && args[0].equalsIgnoreCase("gen")) {
                // 异步获取格言
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    String motto = PlayerJoinListener.fetchMottoFromAI();
                    if (motto != null) {
                        String prefix = getConfig().getString("prefix", "&6今日格言: &f");
                        sender.sendMessage(PlayerJoinListener.colorize(prefix + motto));
                        recentMottoManager.addMotto(motto); // 保存格言
                    } else {
                        sender.sendMessage(PlayerJoinListener.colorize("&c获取格言失败，请稍后再试。"));
                    }
                });
                return true;
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
