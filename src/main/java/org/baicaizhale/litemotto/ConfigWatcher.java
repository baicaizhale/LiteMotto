package org.baicaizhale.litemotto;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * 配置文件监听器，用于监控配置文件变化并自动重载。
 * 实现 Runnable 接口，可在单独的线程中运行。
 */
public class ConfigWatcher implements Runnable {

    private final LiteMotto plugin;
    private final Path configPath;
    private WatchService watcher;
    private boolean running = true;
    private long lastReloadTime = 0;
    private static final long RELOAD_DEBOUNCE_MILLIS = 1000; // 1秒防抖时间

    /**
     * 构造函数，初始化配置文件监听器。
     *
     * @param plugin LiteMotto 插件实例
     * @param configFile 要监听的配置文件
     */
    public ConfigWatcher(LiteMotto plugin, File configFile) {
        this.plugin = plugin;
        this.configPath = configFile.toPath().toAbsolutePath();
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
            // 注册父目录以监听文件修改事件
            this.configPath.getParent().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            DebugManager.sendDebugMessage("&7已开始监听配置文件: &f" + configPath.toString());
        } catch (IOException e) {
            plugin.getLogger().severe("[LiteMotto Debug] 无法初始化配置文件监听器: " + e.getMessage());
        }
    }

    /**
     * 监听线程的执行方法。
     * 持续监听配置文件所在目录的修改事件，并在检测到目标文件修改时重载配置。
     */
    @Override
    public void run() {
        while (running) {
            WatchKey key;
            try {
                // 每秒轮询一次，检查是否有事件发生
                key = watcher.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // 线程中断时，恢复中断状态并退出
                Thread.currentThread().interrupt();
                return;
            }

            if (key != null) {
                // 处理所有待处理的事件
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // 确保是文件修改事件且是目标文件
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changedFile = (Path) event.context();
                        // 检查修改的文件是否是当前监听的配置文件
                        if (configPath.getFileName().equals(changedFile)) {
                            long currentTime = System.currentTimeMillis();
                            // 实现防抖机制，避免短时间内重复重载
                            if (currentTime - lastReloadTime > RELOAD_DEBOUNCE_MILLIS) {
                                lastReloadTime = currentTime;
                                // 在 Bukkit 主线程中重载配置
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    plugin.reloadConfig();
                                    DebugManager.sendDebugMessage(ChatColor.YELLOW + "配置文件已自动重载。");
                                });
                            }
                        }
                    }
                }
                // 重置 WatchKey，使其可以接收后续事件
                key.reset();
            }
        }
    }

    /**
     * 停止配置文件监听器。
     * 设置运行标志为 false，并关闭 WatchService。
     */
    public void stopWatching() {
        this.running = false;
        try {
            // 关闭 WatchService 释放资源
            watcher.close();
            DebugManager.sendDebugMessage("&7配置文件监听器已停止。");
        } catch (IOException e) {
            plugin.getLogger().severe("[LiteMotto Debug] 关闭配置文件监听器失败: " + e.getMessage());
        }
    }
}