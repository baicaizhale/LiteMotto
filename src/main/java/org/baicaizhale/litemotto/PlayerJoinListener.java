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
            String motto = fetchMottoFromAI();
            if (motto != null) {
                String prefix = LiteMotto.getInstance().getConfig().getString("prefix", "&6今日格言: &f");
                player.sendMessage(colorize(prefix + motto));
                LiteMotto.getRecentMottoManager().addMotto(motto); // 保存格言
                
                // 如果玩家处于调试模式，发送详细调试信息
                if (DebugManager.isInDebugMode(player)) {
                    player.sendMessage(colorize("&7[Debug] &a格言生成成功"));
                    player.sendMessage(colorize("&7[Debug] &f内容: &e" + motto));
                    player.sendMessage(colorize("&7[Debug] &f长度: &e" + motto.length() + " &f字符"));
                    player.sendMessage(colorize("&7[Debug] &f已添加到最近格言列表，当前列表大小: &e" + 
                        LiteMotto.getRecentMottoManager().getRecentMottos().size()));
                }
                
                // 如果有调试模式玩家，输出详细调试信息到控制台
                if (DebugManager.hasDebugPlayers()) {
                    Bukkit.getLogger().info("========== LiteMotto 调试信息 ==========");
                    Bukkit.getLogger().info("[Debug] 玩家 " + player.getName() + " 加入服务器");
                    Bukkit.getLogger().info("[Debug] UUID: " + player.getUniqueId());
                    Bukkit.getLogger().info("[Debug] 格言生成成功: " + motto);
                    Bukkit.getLogger().info("[Debug] 格言长度: " + motto.length() + " 字符");
                    Bukkit.getLogger().info("[Debug] 最近格言列表大小: " + LiteMotto.getRecentMottoManager().getRecentMottos().size());
                    Bukkit.getLogger().info("[Debug] 格言生成时间: " + new java.util.Date());
                    Bukkit.getLogger().info("======================================");
                }
            } else {
                player.sendMessage(colorize("&c获取格言失败，请稍后再试。"));
                
                // 如果玩家处于调试模式，发送详细调试信息
                if (DebugManager.isInDebugMode(player)) {
                    player.sendMessage(colorize("&7[Debug] &c格言生成失败"));
                    player.sendMessage(colorize("&7[Debug] &f可能原因: &e网络问题或API响应错误"));
                    player.sendMessage(colorize("&7[Debug] &f请检查控制台获取更多错误信息"));
                }
                
                // 如果有调试模式玩家，输出详细调试信息到控制台
                if (DebugManager.hasDebugPlayers()) {
                    Bukkit.getLogger().info("=========== LiteMotto Debug ===========");
                    Bukkit.getLogger().info("[Debug] 玩家 " + player.getName() + " 加入服务器");
                    Bukkit.getLogger().info("[Debug] UUID: " + player.getUniqueId());
                    Bukkit.getLogger().info("[Debug] 格言生成状态: 失败");
                    Bukkit.getLogger().info("[Debug] 可能原因: 网络问题或API响应错误");
                    Bukkit.getLogger().info("[Debug] 尝试时间: " + new java.util.Date());
                    Bukkit.getLogger().info("======================================");
                }
            }
        });
    }

    public static String fetchMottoFromAI() {
        String lastResponse = null; // 用于异常时打印完整响应
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

            URL url;
            try {
                url = new java.net.URI(apiUrl).toURL();
            } catch (java.net.URISyntaxException e) {
                Bukkit.getLogger().severe("LiteMotto: 无效的API URL格式: " + apiUrl + ", 错误: " + e.getMessage());
                return null;
            }
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
                lastResponse = response.toString();
                JSONObject jsonResponse = new JSONObject(lastResponse);
                // 优先判断 errors 字段
                if (jsonResponse.has("errors") && jsonResponse.getJSONArray("errors").length() > 0) {
                    throw new Exception("Cloudflare API 错误: " + jsonResponse.getJSONArray("errors").toString());
                }
                String motto = null;
                if (isGptOss120b) {
                    // gpt-oss-120b 返回 result.output，需解析 output_text
                    if (jsonResponse.has("result") && jsonResponse.getJSONObject("result").has("output")) {
                        JSONArray outputArr = jsonResponse.getJSONObject("result").getJSONArray("output");
                        for (int i = 0; i < outputArr.length(); i++) {
                            JSONObject outputObj = outputArr.getJSONObject(i);
                            if (outputObj.has("type") && "message".equals(outputObj.getString("type")) && outputObj.has("content")) {
                                JSONArray contentArr = outputObj.getJSONArray("content");
                                for (int j = 0; j < contentArr.length(); j++) {
                                    JSONObject contentObj = contentArr.getJSONObject(j);
                                    if (contentObj.has("type") && "output_text".equals(contentObj.getString("type")) && contentObj.has("text")) {
                                        motto = contentObj.getString("text");
                                        break;
                                    }
                                }
                            }
                            if (motto != null) break;
                        }
                        if (motto == null) {
                            throw new Exception("API 响应未找到 output_text 字段, 原始响应: " + lastResponse);
                        }
                    } else {
                        throw new Exception("API 响应缺少 result.output 字段, 原始响应: " + lastResponse);
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
            if (lastResponse != null) {
                Bukkit.getLogger().severe("Cloudflare 原始响应: " + lastResponse);
            }
            return null;
        }
    }

    // 颜色代码转换方法：将 & 替换成 §
    public static String colorize(String message) {
        return message.replace("&", "§");
    }
}