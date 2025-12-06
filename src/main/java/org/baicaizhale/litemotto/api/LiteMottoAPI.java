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
            ApiConfig apiConfig = loadAndResolveApiConfig();
            if (apiConfig == null) {
                return null;
            }
            String accountId = apiConfig.getAccountId();
            String apiKey = apiConfig.getApiKey();
            String model = apiConfig.getModel();

            boolean isGptOss120b = model != null && model.toLowerCase().contains("gpt-oss-120b");

            // 使用传入的自定义提示词
            String prompt = buildPrompt(customPrompt);

            ApiRequest apiRequest = buildApiRequest(accountId, model, prompt);
            String apiUrl = apiRequest.getApiUrl();
            JSONObject requestBody = apiRequest.getRequestBody();

            HttpResponse httpResponse = executeHttpRequest(apiUrl, apiKey, requestBody);

            if (httpResponse.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 请求 Cloudflare API 失败，HTTP 错误码: " + httpResponse.getResponseCode() + ", 错误信息: " + httpResponse.getErrorBody());
                return null;
            }

            String lastResponse = httpResponse.getResponseBody();
            JSONObject jsonResponse = new JSONObject(lastResponse);

            // 优先判断 errors 字段
            if (jsonResponse.has("errors") && jsonResponse.getJSONArray("errors").length() > 0) {
                throw new Exception("Cloudflare API 错误: " + jsonResponse.getJSONArray("errors").toString());
            }

            ApiResponse apiResponse = parseApiResponse(jsonResponse, isGptOss120b);
            String motto = apiResponse.getMotto();

            return PlayerJoinListener.colorize(motto);
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
            java.net.HttpURLConnection connection = createAccountIdConnection();
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);

            int responseCode = connection.getResponseCode();
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                return readAccountIdResponse(connection);
            } else {
                handleAccountIdError(connection, responseCode);
                return null;
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 获取 Cloudflare 账户 ID 时发生异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 创建获取 Cloudflare 账户 ID 的 HTTP 连接
     * @return 配置好的 HttpURLConnection 对象
     * @throws Exception 如果连接创建失败
     */
    private static java.net.HttpURLConnection createAccountIdConnection() throws Exception {
        java.net.URL url = new java.net.URL("https://api.cloudflare.com/client/v4/accounts");
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        return connection;
    }

    /**
     * 读取并解析 Cloudflare 账户 ID 响应
     * @param connection 已建立的 HTTP 连接
     * @return 解析出的账户 ID；失败返回 null
     * @throws Exception 如果响应读取或解析失败
     */
    private static String readAccountIdResponse(java.net.HttpURLConnection connection) throws Exception {
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
    }

    /**
     * 处理获取 Cloudflare 账户 ID 的 HTTP 错误响应
     * @param connection 已建立的 HTTP 连接
     * @param responseCode HTTP 错误码
     * @throws Exception 如果错误响应读取失败
     */
    private static void handleAccountIdError(java.net.HttpURLConnection connection, int responseCode) throws Exception {
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder errorResponse = new StringBuilder();
            String errorLine;
            while ((errorLine = br.readLine()) != null) {
                errorResponse.append(errorLine.trim());
            }
            org.bukkit.Bukkit.getLogger().severe("LiteMotto API: 获取 Cloudflare 账户 ID 失败，HTTP 错误码: " + responseCode + ", 错误信息: " + errorResponse.toString());
        }
    }

    private static boolean isDemoAccountId(String accountId) {
        return "e607239dba3500c1a85a15d9c37c90d6".equals(accountId);
    }

    /**
     * 检查 API Key 是否为示例 Key。
     * @param apiKey 待检查的 API Key。
     * @return 如果是示例 Key 返回 true，否则返回 false。
     */
    private static boolean isDemoApiKey(String apiKey) {
        return "K_e607239dba3500c1a85a15d9c37c90d6".equals(apiKey);
    }
    /**
     * 封装 HTTP 响应信息。
     */
    private static class HttpResponse {
        private final int responseCode;
        private final String responseBody;
        private final String errorBody;

        public HttpResponse(int responseCode, String responseBody, String errorBody) {
            this.responseCode = responseCode;
            this.responseBody = responseBody;
            this.errorBody = errorBody;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public String getErrorBody() {
            return errorBody;
        }
    }

    /**
     * 执行 HTTP POST 请求并返回响应。
     * @param apiUrl 请求的 URL。
     * @param apiKey 认证用的 API Key。
     * @param requestBody 请求体。
     * @return 包含响应码、响应体和错误体的 HttpResponse 对象。
     * @throws Exception 如果发生网络或 IO 错误。
     */
    private HttpResponse executeHttpRequest(String apiUrl, String apiKey, JSONObject requestBody) throws Exception {
        java.net.URL url = new java.net.URI(apiUrl).toURL();
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);

        try (java.io.OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        StringBuilder errorResponse = new StringBuilder();

        if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
        } else {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String errorLine;
                while ((errorLine = br.readLine()) != null) {
                    errorResponse.append(errorLine.trim());
                }
            }
        }
        return new HttpResponse(responseCode, response.toString(), errorResponse.toString());
    }

    /**
     * 封装 API 响应信息。
     */
    private static class ApiResponse {
        private final String motto;

        public ApiResponse(String motto) {
            this.motto = motto;
        }

        public String getMotto() {
            return motto;
        }
    }

    /**
     * 解析 Cloudflare API 的 JSON 响应，提取格言。
     * @param jsonResponse API 的 JSON 响应。
     * @param isGptOss120b 是否为 gpt-oss-120b 模型。
     * @return 包含解析出的格言的 ApiResponse 对象。
     * @throws Exception 如果响应格式不符合预期或解析失败。
     */
    private ApiResponse parseApiResponse(JSONObject jsonResponse, boolean isGptOss120b) throws Exception {
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
                    throw new Exception("API 响应未找到 output_text 字段, 原始响应: " + jsonResponse.toString());
                }
            } else {
                throw new Exception("API 响应缺少 result.output 字段, 原始响应: " + jsonResponse.toString());
            }
        } else {
            // 其它模型返回 choices[0].message.content
            motto = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            if (motto.startsWith("\u8bf7\u76f4\u63a5\u8fd4\u56de")) {
                motto = motto.substring(8).trim();
            }
        }
        return new ApiResponse(motto);
    }

    /**
     * 封装 API 请求信息。
     */
    private static class ApiRequest {
        private final String apiUrl;
        private final JSONObject requestBody;

        public ApiRequest(String apiUrl, JSONObject requestBody) {
            this.apiUrl = apiUrl;
            this.requestBody = requestBody;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public JSONObject getRequestBody() {
            return requestBody;
        }
    }

    /**
     * 根据模型类型构建 API 请求的 URL 和请求体。
     * @param accountId Cloudflare 账户 ID。
     * @param model 使用的 AI 模型名称。
     * @param finalPrompt 最终的提示词。
     * @return 包含 API URL 和请求体的 ApiRequest 对象。
     */
    private ApiRequest buildApiRequest(String accountId, String model, String finalPrompt) {
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
        return new ApiRequest(apiUrl, requestBody);
    }

    /**
     * 构建最终的提示词，包含避免重复的逻辑。
     * @param customPrompt 用户自定义的提示词。
     * @return 包含避免重复逻辑的最终提示词。
     */
    private String buildPrompt(String customPrompt) {
        // 拼接最近格言，提示AI不要重复
        StringBuilder avoidBuilder = new StringBuilder();
        for (String recent : LiteMotto.getRecentMottoManager().getRecentMottos()) {
            avoidBuilder.append("「").append(recent).append("」, ");
        }
        String avoidText = avoidBuilder.length() > 0
                ? "请不要生成以下内容：" + avoidBuilder.toString() + "。"
                : "";
        return customPrompt + (avoidText.isEmpty() ? "" : "\n" + avoidText);
    }

    /**
     * 封装 API 配置信息。
     */
    private static class ApiConfig {
        private final String accountId;
        private final String apiKey;
        private final String model;

        public ApiConfig(String accountId, String apiKey, String model) {
            this.accountId = accountId;
            this.apiKey = apiKey;
            this.model = model;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getModel() {
            return model;
        }
    }

    /**
     * 加载并解析 API 配置，包括自动获取 account-id。
     * @return 包含 accountId, apiKey, model 的 ApiConfig 对象；如果配置无效则返回 null。
     */
    private ApiConfig loadAndResolveApiConfig() {
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
        return new ApiConfig(accountId, apiKey, model);
    }

}