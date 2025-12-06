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
        handleUpdateCheck(player); // 处理更新检查
        handleMottoGeneration(player); // 处理格言生成
    }

    /**
     * 处理玩家加入时的更新检查逻辑。
     * 如果启用了管理员进服检查更新，并且玩家有权限，则进行更新检查。
     *
     * @param player 加入游戏的玩家
     */
    private void handleUpdateCheck(Player player) {
        // 检查是否启用了管理员进服检查更新功能
        if (LiteMotto.getInstance().getConfig().getBoolean("update-check.on-admin-join", true)) {
            // 检查玩家是否有权限接收更新通知
            if (player.hasPermission("litemotto.update")) {
                // 使用更新检查器检查更新（仅向该玩家发送通知）
                UpdateChecker updateChecker = new UpdateChecker(LiteMotto.getInstance(), true);
                updateChecker.checkForUpdates();
            }
        }
    }

    /**
     * 处理玩家加入时的格言生成和发送逻辑。
     * 异步获取格言，并发送给玩家，同时记录调试信息。
     *
     * @param player 加入游戏的玩家
     */
    private void handleMottoGeneration(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(LiteMotto.getInstance(), () -> {
            String motto = fetchMottoFromAI();
            if (motto != null) {
                String prefix = LiteMotto.getInstance().getConfig().getString("prefix", "&6今日格言: &f");
                player.sendMessage(colorize(prefix + motto));
                LiteMotto.getRecentMottoManager().addMotto(motto); // 保存格言

                // 向所有处于调试模式的玩家发送详细调试信息
                DebugManager.sendDebugMessage("&a格言生成成功");
                DebugManager.sendDebugMessage("&f内容: &e" + motto);
                DebugManager.sendDebugMessage("&f长度: &e" + motto.length() + " &f字符");
                DebugManager.sendDebugMessage("&f已添加到最近格言列表，当前列表大小: &e" + LiteMotto.getRecentMottoManager().getRecentMottos().size());

                // 输出详细调试信息到控制台
                DebugManager.sendDebugMessage("&7========== LiteMotto 调试信息 ==========");
                DebugManager.sendDebugMessage("&7玩家 &f" + player.getName() + " &7加入服务器");
                DebugManager.sendDebugMessage("&7UUID: &f" + player.getUniqueId());
                DebugManager.sendDebugMessage("&a格言生成成功: &f" + motto);
                DebugManager.sendDebugMessage("&7格言长度: &f" + motto.length() + " &7字符");
                DebugManager.sendDebugMessage("&7最近格言列表大小: &f" + LiteMotto.getRecentMottoManager().getRecentMottos().size());
                DebugManager.sendDebugMessage("&7格言生成时间: &f" + new java.util.Date());
                DebugManager.sendDebugMessage("&7======================================");
            } else {
                player.sendMessage(colorize("&c获取格言失败，请稍后再试。"));

                // 向所有处于调试模式的玩家发送详细调试信息
                DebugManager.sendDebugMessage("&c格言生成失败");
                DebugManager.sendDebugMessage("&f可能原因: &e网络问题或API响应错误");
                DebugManager.sendDebugMessage("&f请检查控制台获取更多错误信息");

                // 输出详细调试信息到控制台
                DebugManager.sendDebugMessage("&7=========== LiteMotto Debug ==========");
                DebugManager.sendDebugMessage("&7玩家 &f" + player.getName() + " &7加入服务器");
                DebugManager.sendDebugMessage("&7UUID: &f" + player.getUniqueId());
                DebugManager.sendDebugMessage("&c格言生成状态: 失败");
                DebugManager.sendDebugMessage("&7可能原因: &e网络问题或API响应错误");
                DebugManager.sendDebugMessage("&7尝试时间: &f" + new java.util.Date());
                DebugManager.sendDebugMessage("&7======================================");
            }
        });
    }

    public static String fetchMottoFromAI() {
        String lastResponse = null; // 用于异常时打印完整响应
        try {
            // 从 config.yml 读取配置
            CloudflareAccountInfo accountInfo = getCloudflareAccountInfo();
            if (accountInfo == null) {
                return null;
            }
            String accountId = accountInfo.getAccountId();
            String apiKey = accountInfo.getApiKey();

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

            ApiRequestData requestData = buildApiRequest(accountId, model, finalPrompt);
            if (requestData == null) {
                return null;
            }
            String apiUrl = requestData.getApiUrl();
            JSONObject requestBody = requestData.getRequestBody();
            boolean isGptOss120b = requestData.isGptOss120b();

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
                String motto = parseApiResponse(jsonResponse, isGptOss120b, lastResponse);
                if (motto == null) {
                    throw new Exception("无法从API响应中解析格言。");
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

    /**
     * 封装 API 请求数据。
     */
    private static class ApiRequestData {
        private final String apiUrl;
        private final JSONObject requestBody;
        private final boolean isGptOss120b;

        public ApiRequestData(String apiUrl, JSONObject requestBody, boolean isGptOss120b) {
            this.apiUrl = apiUrl;
            this.requestBody = requestBody;
            this.isGptOss120b = isGptOss120b;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public JSONObject getRequestBody() {
            return requestBody;
        }

        public boolean isGptOss120b() {
            return isGptOss120b;
        }
    }

    /**
     * 构建 API 请求的 URL 和请求体。
     *
     * @param accountId Cloudflare 账户 ID
     * @param model AI 模型名称
     * @param finalPrompt 最终的提示语
     * @return 包含 API URL、请求体和模型类型的 ApiRequestData 对象，如果构建失败则返回 null。
     */
    private static ApiRequestData buildApiRequest(String accountId, String model, String finalPrompt) {
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
        return new ApiRequestData(apiUrl, requestBody, isGptOss120b);
    }

    /**
     * 解析 API 响应，提取格言。
     *
     * @param jsonResponse API 响应的 JSON 对象
     * @param isGptOss120b 是否为 gpt-oss-120b 模型
     * @param rawResponse 原始的 API 响应字符串
     * @return 解析出的格言，如果解析失败则返回 null。
     * @throws Exception 如果解析过程中发生错误
     */
    private static String parseApiResponse(JSONObject jsonResponse, boolean isGptOss120b, String rawResponse) throws Exception {
        String motto = null;
        if (isGptOss120b) {
            // gpt-oss-120b 返回 result.output，需解析 output_text
            if (jsonResponse.has("result") && jsonResponse.getJSONObject("result").has("output")) {
                JSONArray outputArr = jsonResponse.getJSONObject("result").getJSONArray("output");
                for (int i = 0; i < outputArr.length(); i++) {
                    JSONObject outputObj = outputArr.getJSONObject(i);
                    if (outputObj.has("type") && "message".equals(outputObj.getString("type")) && outputObj.has("content")) {
                        JSONArray contentArr = outputObj.getJSONArray( "content");
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
                    throw new Exception("API 响应未找到 output_text 字段, 原始响应: " + rawResponse);
                }
            } else {
                throw new Exception("API 响应缺少 result.output 字段, 原始响应: " + rawResponse);
            }
        } else {
            // 其它模型返回 choices[0].message.content
            motto = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            if (motto.startsWith("</think>")) {
                motto = motto.substring(8).trim();
            }
        }
        return motto;
    }

    /**
     * 从配置文件中获取 Cloudflare 账户信息。
     * 如果 accountId 未配置，则尝试通过 API Key 自动获取。
     *
     * @return CloudflareAccountInfo 对象，如果获取失败则返回 null。
     */
    private static CloudflareAccountInfo getCloudflareAccountInfo() {
        String accountId = LiteMotto.getInstance().getConfig().getString("cloudflare.accountId");
        String apiKey = LiteMotto.getInstance().getConfig().getString("cloudflare.apiKey");

        if (apiKey == null || apiKey.isEmpty()) {
            Bukkit.getLogger().severe("LiteMotto: Cloudflare API Key 未配置。请在 config.yml 中设置 'cloudflare.apiKey'。");
            return null;
        }

        if (accountId == null || accountId.isEmpty()) {
            Bukkit.getLogger().warning("LiteMotto: Cloudflare Account ID 未配置。尝试通过 API Key 自动获取...");
            accountId = getAccountIdFromCloudflare(apiKey);
            if (accountId == null) {
                Bukkit.getLogger().severe("LiteMotto: 无法自动获取 Cloudflare Account ID。请在 config.yml 中手动设置 'cloudflare.accountId'。");
                return null;
            }
            LiteMotto.getInstance().getConfig().set("cloudflare.accountId", accountId);
            LiteMotto.getInstance().saveConfig();
            Bukkit.getLogger().info("LiteMotto: 已自动获取并保存 Cloudflare Account ID: " + accountId);
        }
        return new CloudflareAccountInfo(accountId, apiKey);
    }

    /**
     * 通过 Cloudflare API Key 自动获取账户 ID。
     *
     * @param apiKey Cloudflare API Key
     * @return 账户 ID，如果获取失败则返回 null
     */
    private static String getAccountIdFromCloudflare(String apiKey) {
        try {
            URL url = new URL("https://api.cloudflare.com/client/v4/accounts");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                        JSONArray result = jsonResponse.getJSONArray("result");
                        if (result.length() > 0) {
                            return result.getJSONObject(0).getString("id");
                        } else {
                            Bukkit.getLogger().warning("LiteMotto: Cloudflare API 返回的账户列表为空。");
                            return null;
                        }
                    } else {
                        String errors = jsonResponse.has("errors") ? jsonResponse.getJSONArray("errors").toString() : "未知错误";
                        Bukkit.getLogger().severe("LiteMotto: 获取 Cloudflare 账户 ID 失败，API 响应错误: " + errors);
                        return null;
                    }
                }
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine.trim());
                    }
                    Bukkit.getLogger().severe("LiteMotto: 获取 Cloudflare 账户 ID 失败，HTTP 错误码: " + responseCode + ", 错误信息: " + errorResponse.toString());
                }
                return null;
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("LiteMotto: 获取 Cloudflare 账户 ID 时发生异常: " + e.getMessage());
            return null;
        }
    }

    // 颜色代码转换方法：将 & 替换成 §
    public static String colorize(String message) {
        return message.replace("&", "§");
    }

    /**
     * 将带有Minecraft颜色代码的字符串转换为带有ANSI颜色代码的字符串，用于控制台输出。
     * @param message 带有Minecraft颜色代码的字符串。
     * @return 带有ANSI颜色代码的字符串。
     */
    public static String toAnsiColor(String message) {
        String coloredMessage = colorize(message); // 先将 & 转换为 §
        StringBuilder ansiMessage = new StringBuilder();
        for (int i = 0; i < coloredMessage.length(); i++) {
            char c = coloredMessage.charAt(i);
            if (c == '§' && i + 1 < coloredMessage.length()) {
                char colorCode = coloredMessage.charAt(i + 1);
                switch (colorCode) {
                    case '0': ansiMessage.append("\u001B[30m"); break; // Black
                    case '1': ansiMessage.append("\u001B[34m"); break; // Dark Blue
                    case '2': ansiMessage.append("\u001B[32m"); break; // Dark Green
                    case '3': ansiMessage.append("\u001B[36m"); break; // Dark Aqua
                    case '4': ansiMessage.append("\u001B[31m"); break; // Dark Red
                    case '5': ansiMessage.append("\u001B[35m"); break; // Dark Purple
                    case '6': ansiMessage.append("\u001B[33m"); break; // Gold
                    case '7': ansiMessage.append("\u001B[37m"); break; // Gray
                    case '8': ansiMessage.append("\u001B[90m"); break; // Dark Gray
                    case '9': ansiMessage.append("\u001B[94m"); break; // Blue
                    case 'a': ansiMessage.append("\u001B[92m"); break; // Green
                    case 'b': ansiMessage.append("\u001B[96m"); break; // Aqua
                    case 'c': ansiMessage.append("\u001B[91m"); break; // Red
                    case 'd': ansiMessage.append("\u001B[95m"); break; // Light Purple
                    case 'e': ansiMessage.append("\u001B[93m"); break; // Yellow
                    case 'f': ansiMessage.append("\u001B[97m"); break; // White
                    case 'r': ansiMessage.append("\u001B[0m"); break;  // Reset
                    default: ansiMessage.append(c); // If not a recognized color code, append as is
                }
                i++; // Skip the color code character
            } else {
                ansiMessage.append(c);
            }
        }
        ansiMessage.append("\u001B[0m"); // Reset color at the end
        return ansiMessage.toString();
    }
}