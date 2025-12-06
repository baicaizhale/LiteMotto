package org.baicaizhale.litemotto;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/baicaizhale/LiteMotto/releases/latest";
    private static final String[] MIRROR_URLS = {
            "https://hub.gitmirror.com/",
            "https://ghproxy.com/"
    };
    
    private final JavaPlugin plugin;
    private final boolean checkOnJoin;
    private final boolean isManualCheck;
    
    public UpdateChecker(JavaPlugin plugin, boolean checkOnJoin, boolean isManualCheck) {
        this.plugin = plugin;
        this.checkOnJoin = checkOnJoin;
        this.isManualCheck = isManualCheck;
    }
    
    // 兼容旧构造函数
    public UpdateChecker(JavaPlugin plugin, boolean checkOnJoin) {
        this(plugin, checkOnJoin, false);
    }
    
    /**
     * 检查更新
     */
    public void checkForUpdates() {
        checkForUpdates(null);
    }
    
    /**
     * 检查更新并向指定玩家发送信息
     * @param player 要接收更新信息的玩家，如果为null则使用默认行为
     */
    public void checkForUpdates(Player player) {
        // 手动检查不受配置限制
        if (!isManualCheck) {
            if (!plugin.getConfig().getBoolean("update-check.enabled", true)) {
                return;
            }
        }
        
        // 检查是否为快照版本，如果是则跳过更新检查
        String localVersion = plugin.getDescription().getVersion();
        if (localVersion.startsWith("#")) {
            // 快照版本不检查更新
            if (isManualCheck) {
                if (player != null) {
                    player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &e当前为快照版本，跳过更新检查"));
                } else {
                    plugin.getLogger().info("当前为快照版本，跳过更新检查");
                }
            }
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                JSONObject releaseInfo = getLatestReleaseInfo();
                
                if (releaseInfo == null) {
                    // 只有在手动检查或自动检查出错时才记录日志
                    if (isManualCheck) {
                        if (player != null) {
                            player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &c检查更新失败，无法获取最新版本信息"));
                        } else {
                            plugin.getLogger().info("检查更新失败，无法获取最新版本信息");
                        }
                    } else {
                        plugin.getLogger().warning("自动检查更新失败，无法获取最新版本信息");
                    }
                    return;
                }
                
                String remoteVersionRaw = releaseInfo.getString("tag_name");
                String remoteVersion = remoteVersionRaw.startsWith("v") ? 
                        remoteVersionRaw.substring(1) : remoteVersionRaw;
                
                // 比较版本号
                int versionComparison = compareVersions(localVersion, remoteVersion);

                if (versionComparison < 0) { // 本地版本低于远程版本，有新版本
                    // 继续执行下面的新版本提醒逻辑
                } else if (versionComparison > 0) { // 本地版本高于远程版本
                    if (isManualCheck) {
                        if (player != null) {
                            player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &eLiteMotto 插件版本 (&f" + localVersion + "&e) 高于最新版本 (&f" + remoteVersion + "&e)"));
                        } else {
                            plugin.getLogger().info("LiteMotto 插件版本 (" + localVersion + ") 高于最新版本 (" + remoteVersion + ")");
                        }
                    }
                    return;
                } else { // 版本相同
                    if (isManualCheck) {
                        if (player != null) {
                            player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &eLiteMotto 插件已是最新版本 (&f" + localVersion + "&e)"));
                        } else {
                            plugin.getLogger().info("LiteMotto 插件已是最新版本 (" + localVersion + ")");
                        }
                    }
                    return;
                }
                
                // 获取下载链接
                String downloadUrl = null;
                if (releaseInfo.has("assets") && releaseInfo.getJSONArray("assets").length() > 0) {
                    JSONObject asset = releaseInfo.getJSONArray("assets").getJSONObject(0);
                    downloadUrl = asset.getString("browser_download_url");
                    
                    if (asset.has("digest")) {
                    }
                }
                
                // 构造镜像链接
                String mirrorUrl = null;
                if (downloadUrl != null && MIRROR_URLS.length > 0) {
                    mirrorUrl = MIRROR_URLS[0] + downloadUrl;
                }
                
                // 控制台提醒（发现新版本时总是提醒）
                if (player != null) {
                    player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &e发现新版本: &f" + remoteVersion + " &e(当前版本: &f" + localVersion + "&e)"));
                    player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &e原始下载链接: &f" + downloadUrl));
                    if (mirrorUrl != null) {
                        player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &e镜像下载链接: &f" + mirrorUrl));
                    }
                } else {
                    plugin.getLogger().info("发现新版本: " + remoteVersion + " (当前版本: " + localVersion + ")");
                    plugin.getLogger().info("原始下载链接: " + downloadUrl);
                    if (mirrorUrl != null) {
                        plugin.getLogger().info("镜像下载链接: " + mirrorUrl);
                    }
                    
                    // 向有权限的在线玩家发送提醒
                    if (checkOnJoin) {
                        String finalDownloadUrl = downloadUrl;
                        String finalMirrorUrl = mirrorUrl;
                        Bukkit.getScheduler().runTask(plugin, () -> 
                                notifyPlayers(remoteVersion, localVersion, finalDownloadUrl, finalMirrorUrl));
                    }
                }
                
            } catch (Exception e) {
                // 只有在手动检查或自动检查出错时才记录日志
                if (isManualCheck) {
                    if (player != null) {
                        player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &c检查更新时发生错误: " + e.getMessage()));
                    } else {
                        plugin.getLogger().warning("检查更新时发生错误: " + e.getMessage());
                    }
                } else {
                    plugin.getLogger().warning("自动检查更新时发生错误: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 获取最新版本信息
     */
    private JSONObject getLatestReleaseInfo() throws IOException {
        URL url = new URL(GITHUB_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "LiteMotto-Update-Checker");
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            // 重试一次
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "LiteMotto-Update-Checker");
            responseCode = connection.getResponseCode();
            
            if (responseCode != 200) {
                throw new IOException("HTTP响应码: " + responseCode);
            }
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return new JSONObject(response.toString());
    }
    
    /**
     * 向有权限的玩家发送更新提醒
     */
    private void notifyPlayers(String remoteVersion, String localVersion, String downloadUrl, String mirrorUrl) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("litemotto.update")) {
                player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &e发现新版本 &f" + remoteVersion + " &e(当前版本: &f" + localVersion + "&e)"));
                if (downloadUrl != null) {
                    player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &e原始下载链接: &f" + downloadUrl));
                }
                if (mirrorUrl != null) {
                    player.sendMessage(PlayerJoinListener.colorize("&6[LiteMotto] &e镜像下载链接: &f" + mirrorUrl));
                }
            }
        }
    }

    /**
     * 比较两个版本字符串。
     * @param v1 版本字符串1
     * @param v2 版本字符串2
     * @return 如果 v1 > v2 返回正数，如果 v1 < v2 返回负数，如果 v1 = v2 返回 0
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 < p2) {
                return -1;
            }
            if (p1 > p2) {
                return 1;
            }
        }
        return 0;
    }

}