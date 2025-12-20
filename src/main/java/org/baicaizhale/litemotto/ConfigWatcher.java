package org.baicaizhale.litemotto;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class ConfigWatcher implements Runnable {

    private final LiteMotto plugin;
    private final Path configPath;
    private WatchService watcher;
    private boolean running = true;
    private long lastReloadTime = 0;
    private static final long RELOAD_DEBOUNCE_MILLIS = 1000; // 1秒防抖时间

    public ConfigWatcher(LiteMotto plugin, File configFile) {
        this.plugin = plugin;
        this.configPath = configFile.toPath().toAbsolutePath();
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
            // 注册父目录以监听文件修改事件
            this.configPath.getParent().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            DebugManager.sendDebugMessage("&7已开始监听配置文件: &f" + configPath.toString());
        } catch (IOException e) {
            plugin.getLogger().severe("&7无法初始化配置文件监听器: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        if (watcher == null) return;
        while (running) {
            WatchKey key;
            try {
                key = watcher.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException e) {
                // 如果 WatchService 被关闭，优雅地退出循环
                running = false;
                break;
            }

            if (key != null && running) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // 确保是文件修改事件且是目标文件
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changedFile = (Path) event.context();
                        if (configPath.getFileName().equals(changedFile)) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastReloadTime > RELOAD_DEBOUNCE_MILLIS) {
                                lastReloadTime = currentTime;
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    plugin.reloadConfig();
                                    DebugManager.sendDebugMessage(ChatColor.YELLOW + "配置文件已自动重载。");
                                });
                            }
                        }
                    }
                }
                key.reset();
            }
        }
    }

    public void stopWatching() {
        this.running = false;
        if (watcher != null) {
            try {
                watcher.close();
                DebugManager.sendDebugMessage("&7配置文件监听器已停止。");
            } catch (IOException e) {
                plugin.getLogger().severe("&7关闭配置文件监听器失败: " + e.getMessage());
            }
        }
    }
}