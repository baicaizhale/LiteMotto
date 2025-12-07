package org.baicaizhale.litemotto.api;

import org.baicaizhale.litemotto.LiteMotto;
import org.bukkit.Bukkit;

/**
 * LiteMotto插件API接口
 * 为其他插件提供调用LiteMotto功能的接口
 */
public class LiteMottoAPI {

    private MottoGenerator currentGenerator;

    public LiteMottoAPI() {
        initMottoGenerator();
    }

    /**
     * 初始化格言生成器
     * 根据配置文件选择并实例化相应的API平台实现
     */
    public void initMottoGenerator() {
        String apiProvider = LiteMotto.getInstance().getConfig().getString("api-provider", "cloudflare").toLowerCase();
        switch (apiProvider) {
            case "cloudflare":
                String accountId = LiteMotto.getInstance().getConfig().getString("account-id");
                String apiKey = LiteMotto.getInstance().getConfig().getString("api-key");
                String model = LiteMotto.getInstance().getConfig().getString("model");
                this.currentGenerator = new CloudflareMottoGenerator(accountId, apiKey, model);
                Bukkit.getLogger().info("LiteMotto API: 已选择 Cloudflare 作为格言生成器。");
                break;
            case "siliconflow":
                apiKey = LiteMotto.getInstance().getConfig().getString("siliconflow.api-key");
                model = LiteMotto.getInstance().getConfig().getString("siliconflow.model");
                String apiUrl = LiteMotto.getInstance().getConfig().getString("siliconflow.api-url", "https://api.siliconflow.cn/v1/chat/completions");
                this.currentGenerator = new SiliconFlowMottoGenerator(apiKey, model, apiUrl);
                Bukkit.getLogger().info("LiteMotto API: 已选择 SiliconFlow 作为格言生成器。");
                break;
            default:
                Bukkit.getLogger().severe("LiteMotto API: 未知的 API 提供商: " + apiProvider + "。将使用 Cloudflare 作为默认提供商。");
                // 默认使用 Cloudflare
                accountId = LiteMotto.getInstance().getConfig().getString("account-id");
                apiKey = LiteMotto.getInstance().getConfig().getString("api-key");
                model = LiteMotto.getInstance().getConfig().getString("model");
                this.currentGenerator = new CloudflareMottoGenerator(accountId, apiKey, model);
                break;
        }
    }

    /**
     * 使用自定义提示词获取格言
     * @param customPrompt 自定义提示词
     * @return 生成的格言；失败返回null
     */
    public String fetchMottoWithPrompt(String customPrompt) {
        if (currentGenerator == null) {
            Bukkit.getLogger().severe("LiteMotto API: 格言生成器未初始化。");
            return null;
        }
        return currentGenerator.fetchMotto(customPrompt);
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
