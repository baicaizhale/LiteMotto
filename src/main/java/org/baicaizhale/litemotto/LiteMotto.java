package org.baicaizhale.litemotto;

import org.baicaizhale.litemotto.Metrics.Metrics; // 添加 bStats 导入
import org.bukkit.plugin.java.JavaPlugin;

public class LiteMotto extends JavaPlugin {
    private static LiteMotto instance;
    private static RecentMottoManager recentMottoManager;

    @Override
    public void onEnable() {
        instance = this;
        recentMottoManager = new RecentMottoManager(10); // 保存最近10条
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        saveDefaultConfig();

        // 初始化 bStats
        int pluginId = 25873; // bStats 插件 ID
        Metrics metrics = new Metrics(this, pluginId);

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
