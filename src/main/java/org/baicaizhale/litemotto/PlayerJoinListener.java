package org.baicaizhale.litemotto; // 确保和文件夹名匹配

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(LiteMotto.getInstance(), () -> {
            String motto = fetchMottoFromDeepSeek();
            if (motto != null) {
                String prefix = LiteMotto.getInstance().getConfig().getString("prefix", "&6今日格言: &f");
                player.sendMessage(colorize(prefix + motto));
            } else {
                player.sendMessage(colorize("&c获取格言失败，请稍后再试。"));
            }
        });
    }

    private String fetchMottoFromDeepSeek() {
        try {
            // 从 config.yml 读取配置
            String accountId = LiteMotto.getInstance().getConfig().getString("account-id");
            String apiKey = LiteMotto.getInstance().getConfig().getString("api-key");
            String model = LiteMotto.getInstance().getConfig().getString("model");
            String prompt = LiteMotto.getInstance().getConfig().getString("prompt", "请生成一条简短的格言");

            // 构造 API 请求 URL
            String apiUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/v1/chat/completions";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            // 发送 JSON 请求
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("messages", new JSONArray().put(
                    new JSONObject().put("role", "user").put("content", prompt)
            ));

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取 API 响应
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // 解析 JSON 响应
                JSONObject jsonResponse = new JSONObject(response.toString());
                String motto = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

                // 去掉 "</think>" 前缀
                if (motto.startsWith("</think>")) {
                    motto = motto.substring(8).trim();
                }

                return colorize(motto);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("获取格言失败: " + e.getMessage());
            return null;
        }
    }

    // 颜色代码转换方法：将 & 替换成 §
    private String colorize(String message) {
        return message.replace("&", "§");
    }
}
