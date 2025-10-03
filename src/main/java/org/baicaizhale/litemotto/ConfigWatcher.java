package org.baicaizhale.litemotto;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class ConfigWatcher implements Runnable {

    private final LiteMotto plugin;
    private final Path configPath;
    private WatchService watcher;
    private boolean running = true;

    public ConfigWatcher(LiteMotto plugin, File configFile) {
        this.plugin = plugin;
        this.configPath = configFile.toPath().toAbsolutePath();
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
            // 注册父目录以监听文件修改事件
            this.configPath.getParent().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            plugin.getLogger().info("[LiteMotto Debug] 已开始监听配置文件: " + configPath.toString());
        } catch (IOException e) {
            plugin.getLogger().severe("[LiteMotto Debug] 无法初始化配置文件监听器: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        while (running) {
            WatchKey key;
            try {
                key = watcher.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // 确保是文件修改事件且是目标文件
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changedFile = (Path) event.context();
                        if (configPath.getFileName().equals(changedFile)) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.reloadConfig();
                                plugin.getLogger().info("[LiteMotto Debug] 配置文件已自动重载。");
                            });
                        }
                    }
                }
                key.reset();
            }
        }
    }

    public void stopWatching() {
        this.running = false;
        try {
            watcher.close();
            plugin.getLogger().info("[LiteMotto Debug] 配置文件监听器已停止。");
        } catch (IOException e) {
            plugin.getLogger().severe("[LiteMotto Debug] 关闭配置文件监听器失败: " + e.getMessage());
        }
    }
}