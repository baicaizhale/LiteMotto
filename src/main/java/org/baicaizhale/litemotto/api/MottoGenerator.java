package org.baicaizhale.litemotto.api;

/**
 * 格言生成器接口
 * 定义了不同API平台生成格言的方法
 */
public interface MottoGenerator {
    /**
     * 根据提示词获取格言
     * @param prompt 提示词
     * @return 生成的格言；失败返回null
     */
    String fetchMotto(String prompt);

    /**
     * 获取使用的模型名称
     * @return 模型名称
     */
    String getModelName();

    /**
     * 获取模型提供商名称
     * @return 提供商名称
     */
    String getProviderName();
}
