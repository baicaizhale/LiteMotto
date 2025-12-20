package org.baicaizhale.litemotto;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 配置文件自动更新类
 * 负责在插件版本更新时，保留旧配置并迁移到新格式
 */
public class ConfigUpdater {

    private final JavaPlugin plugin;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public ConfigUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 执行配置文件更新逻辑
     * 1. 获取当前版本号
     * 2. 比较版本号，如果不一样或不存在则执行更新
     * 3. 读取 jar 包内的资源文件写入 temp.yml
     * 4. 合并旧配置到 temp.yml
     * 5. 备份旧 config.yml 并替换
     */
    public void updateConfig() {
        String currentVersion = plugin.getDescription().getVersion();
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // 如果文件不存在，直接保存并结束
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String configVersion = config.getString("version");

        // 如果版本号不一样或者不存在
        if (configVersion == null || !configVersion.equals(currentVersion)) {
            plugin.getLogger().info("检测到版本更新，正在自动迁移配置...");
            
            File tempFile = new File(plugin.getDataFolder(), "temp.yml");
            File oldConfigFile = new File(plugin.getDataFolder(), "config.yml.old");

            try {
                // 3. 读取 jar 包内的资源文件，写入到 temp.yml
                try (InputStream in = plugin.getResource("config.yml")) {
                    if (in != null) {
                        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        plugin.getLogger().severe("无法从 JAR 包中读取默认 config.yml！");
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
                        plugin.getLogger().info("配置文件更新成功！旧配置已备份为 config.yml.old");
                    } else {
                        plugin.getLogger().severe("无法将 temp.yml 重命名为 config.yml！");
                    }
                } else {
                    plugin.getLogger().severe("无法备份旧的 config.yml！");
                }

            } catch (IOException e) {
                plugin.getLogger().severe("更新配置文件时发生错误: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 清理临时文件
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
            
            // 重新加载配置到内存
            plugin.reloadConfig();
        }
    }
}
