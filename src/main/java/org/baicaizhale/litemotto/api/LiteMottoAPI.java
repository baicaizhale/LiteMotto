package org.baicaizhale.litemotto.api;

import org.baicaizhale.litemotto.LiteMotto;
import org.baicaizhale.litemotto.PlayerJoinListener;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * LiteMotto插件API接口
 * 为其他插件提供调用LiteMotto功能的接口
 */
public class LiteMottoAPI {

    /**
     * 使用自定义提示词获取格言
     * 当 account-id 未配置或为示例ID时，将尝试通过 api-key 自动获取后再调用 Cloudflare。
     * @param customPrompt 自定义提示词
     * @return 生成的格言；失败返回null
     */
    public String fetchMottoWithPrompt(String customPrompt) {
        try {
            // 从 config.yml 读取配置
            String accountId = LiteMotto.getInstance().getConfig().getString("account-id");
            String apiKey = LiteMotto.getInstance().getConfig().getString("api-key");
            String model = LiteMotto.getInstance().getConfig().getString("model");

            // 如果 accountId 未配置或为示例ID，则尝试通过 API Key 自动获取
            if (accountId == null || accountId.isEmpty() || isDemoAccountId(accountId)) {
                if (apiKey == null || apiKey.isEmpty() || isDemoApiKey(apiKey)) {
                    org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 未配置有效的 Cloudflare API Key，且 account-id 未指定。请在 config.yml 中配置 api-key。");
                    return null;
                }
                org.baicaizhale.litemotto.DebugManager.sendDebugMessage("&e尝试通过 Cloudflare API Key 自动获取 account-id...");
                String resolved = getAccountIdFromCloudflare(apiKey);
                if (resolved == null) {
                    org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 无法通过 Cloudflare API Key 自动获取 account-id。请检查 API Key 是否有效或手动配置 account-id。");
                    return null;
                }
                accountId = resolved;
                org.baicaizhale.litemotto.DebugManager.sendDebugMessage("&a成功获取到 account-id: &f" + accountId);
            }

            // 使用传入的自定义提示词
            String prompt = customPrompt;

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

            java.net.URL url;
            try {
                url = new java.net.URI(apiUrl).toURL();
            } catch (java.net.URISyntaxException e) {
                org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 无效的API URL格式: " + apiUrl + ", 错误: " + e.getMessage());
                return null;
            }
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String lastResponse = response.toString();
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
                    if (motto.startsWith("\u8bf7\u76f4\u63a5\u8fd4\u56de")) {
                        motto = motto.substring(8).trim();
                    }
                }

                return PlayerJoinListener.colorize(motto);
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("LiteMotto API 获取格言失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查LiteMotto插件是否可用
     * @return 如果可用返回true，否则返回false
     */
    public boolean isAvailable() {
        return LiteMotto.getInstance() != null &&
               LiteMotto.getInstance().isEnabled();
    }

    /**
     * 通过 Cloudflare API Key 自动获取账户 ID
     * @param apiKey Cloudflare API Key
     * @return 账户 ID；获取失败返回 null
     */
    private static String getAccountIdFromCloudflare(String apiKey) {
        try {
            java.net.URL url = new java.net.URL("https://api.cloudflare.com/client/v4/accounts");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
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
                            org.bukkit.Bukkit.getLogger().warning("LiteMotto API: Cloudflare API 返回的账户列表为空。");
                            return null;
                        }
                    } else {
                        String errors = jsonResponse.has("errors") ? jsonResponse.getJSONArray("errors").toString() : "未知错误";
                        org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 获取 Cloudflare 账户 ID 失败，API 响应错误: " + errors);
                        return null;
                    }
                }
            } else {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine.trim());
                    }
                    org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 获取 Cloudflare 账户 ID 失败，HTTP 错误码: " + responseCode + ", 错误信息: " + errorResponse.toString());
                }
                return null;
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 获取 Cloudflare 账户 ID 时发生异常: " + e.getMessage());
            return null;
        }
    }

    private static boolean isDemoAccountId(String accountId) {
        return "e607239dba3500c1a85a15d9c37c90d6".equals(accountId);
    }

    private static boolean isDemoApiKey(String apiKey) {
        return "8Dymw2YgadUf2pObmOmu-W_pkFvC1HmulkBSc7pV".equals(apiKey);
    }
}
