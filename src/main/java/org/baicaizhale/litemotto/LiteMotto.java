package org.baicaizhale.litemotto;

import org.bukkit.plugin.java.JavaPlugin;

public class LiteMotto extends JavaPlugin {
    private static LiteMotto instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static LiteMotto getInstance() {
        return instance;
    }
}