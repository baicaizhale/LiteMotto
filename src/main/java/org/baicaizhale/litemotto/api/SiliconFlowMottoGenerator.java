package org.baicaizhale.litemotto.api;

import org.baicaizhale.litemotto.LiteMotto;
import org.bukkit.Bukkit;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 硅基流动格言生成器
 * 实现了MottoGenerator接口，用于通过硅基流动API生成格言
 */
public class SiliconFlowMottoGenerator implements MottoGenerator {
    private final String apiKey;
    private final String model;
    private final String apiUrl;

    /**
     * 构造函数
     * @param apiKey 硅基流动API Key
     * @param model 使用的模型
     * @param apiUrl API请求地址
     */
    public SiliconFlowMottoGenerator(String apiKey, String model, String apiUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    /**
     * 根据提示词获取格言
     * @param prompt 提示词
     * @return 生成的格言；失败返回null
     */
    @Override
    public String fetchMotto(String prompt) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            JSONArray messages = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个格言生成器，根据用户提供的提示词生成一句格言。");
            messages.put(systemMessage);
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            requestBody.put("messages", messages);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject message = firstChoice.getJSONObject("message");
                        return message.getString("content");
                    }
                }
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine.trim());
                    }
                    Bukkit.getLogger().severe("SiliconFlow API 请求失败，响应码: " + responseCode + ", 错误信息: " + errorResponse.toString());
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("SiliconFlow API 请求异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
