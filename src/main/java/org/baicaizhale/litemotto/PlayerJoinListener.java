package org.baicaizhale.litemotto;

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
                LiteMotto.getRecentMottoManager().addMotto(motto); // 保存格言
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

            // 拼接最近格言，提示AI不要重复
            StringBuilder avoidBuilder = new StringBuilder();
            for (String recent : LiteMotto.getRecentMottoManager().getRecentMottos()) {
                avoidBuilder.append("「").append(recent).append("」, ");
            }
            String avoidText = avoidBuilder.length() > 0
                    ? "请不要生成以下内容：" + avoidBuilder.toString() + "。"
                    : "";
            String finalPrompt = prompt + (avoidText.isEmpty() ? "" : "\n" + avoidText);

            boolean isGptOss120b = model != null && model.toLowerCase().contains("gpt-oss-120b");
            String apiUrl;
            JSONObject requestBody = new JSONObject();
            if (isGptOss120b) {
                // gpt-oss-120b 走 run 接口，input 格式
                apiUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/@cf/openai/gpt-oss-120b";
                requestBody.put("input", finalPrompt);
            } else {
                // 其它模型走 chat/completions
                apiUrl = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/v1/chat/completions";
                requestBody.put("model", model);
                requestBody.put("messages", new JSONArray().put(
                        new JSONObject().put("role", "user").put("content", finalPrompt)
                ));
            }

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                JSONObject jsonResponse = new JSONObject(response.toString());
                String motto;
                if (isGptOss120b) {
                    // gpt-oss-120b 返回 result.response
                    if (jsonResponse.has("result") && jsonResponse.getJSONObject("result").has("response")) {
                        motto = jsonResponse.getJSONObject("result").getString("response");
                    } else {
                        throw new Exception("API 响应缺少 result.response 字段");
                    }
                } else {
                    // 其它模型返回 choices[0].message.content
                    motto = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    if (motto.startsWith("</think>")) {
                        motto = motto.substring(8).trim();
                    }
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