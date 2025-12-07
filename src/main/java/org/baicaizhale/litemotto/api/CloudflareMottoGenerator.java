package org.baicaizhale.litemotto.api;

import org.baicaizhale.litemotto.LiteMotto;
import org.baicaizhale.litemotto.PlayerJoinListener;
import org.baicaizhale.litemotto.api.MottoGenerator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Cloudflare API 的格言生成器实现
 */
public class CloudflareMottoGenerator implements MottoGenerator {

    private final String accountId;
    private final String apiKey;
    private final String model;

    public CloudflareMottoGenerator(String accountId, String apiKey, String model) {
        this.accountId = accountId;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String fetchMotto(String prompt) {
        try {
            String resolvedAccountId = this.accountId;
            // 如果 accountId 未配置或为示例ID，则尝试通过 API Key 自动获取
            if (resolvedAccountId == null || resolvedAccountId.isEmpty() || isDemoAccountId(resolvedAccountId)) {
                if (apiKey == null || apiKey.isEmpty() || isDemoApiKey(apiKey)) {
                    org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 未配置有效的 Cloudflare API Key，且 account-id 未指定。请在 config.yml 中配置 api-key。");
                    return null;
                }
                org.baicaizhale.litemotto.DebugManager.sendDebugMessage("&e尝试通过 Cloudflare API Key 自动获取 account-id...");
                String fetchedAccountId = getAccountIdFromCloudflare(apiKey);
                if (fetchedAccountId == null) {
                    org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 无法通过 Cloudflare API Key 自动获取 account-id。请检查 API Key 是否有效或手动配置 account-id。");
                    return null;
                }
                resolvedAccountId = fetchedAccountId;
                org.baicaizhale.litemotto.DebugManager.sendDebugMessage("&a成功获取到 account-id: &f" + resolvedAccountId);
            }

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
                apiUrl = "https://api.cloudflare.com/client/v4/accounts/" + resolvedAccountId + "/ai/run/@cf/openai/gpt-oss-120b";
                requestBody.put("input", finalPrompt);
            } else {
                // 其它模型走 chat/completions
                apiUrl = "https://api.cloudflare.com/client/v4/accounts/" + resolvedAccountId + "/ai/v1/chat/completions";
                requestBody.put("model", model);
                requestBody.put("messages", new JSONArray().put(
                        new JSONObject().put("role", "user").put("content", finalPrompt)
                ));
            }

            URL url;
            try {
                url = new URI(apiUrl).toURL();
            } catch (URISyntaxException e) {
                org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 无效的API URL格式: " + apiUrl + ", 错误: " + e.getMessage());
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
     * 通过 Cloudflare API Key 自动获取账户 ID
     * @param apiKey Cloudflare API Key
     * @return 账户 ID；获取失败返回 null
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
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
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
