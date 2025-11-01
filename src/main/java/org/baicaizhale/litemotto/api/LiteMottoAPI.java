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
     * @param customPrompt 自定义提示词
     * @return 生成的格言，如果失败返回null
     */
    public String fetchMottoWithPrompt(String customPrompt) {
        try {
            // 从 config.yml 读取配置
            String accountId = LiteMotto.getInstance().getConfig().getString("account-id");
            String apiKey = LiteMotto.getInstance().getConfig().getString("api-key");
            String model = LiteMotto.getInstance().getConfig().getString("model");
            
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
}