package org.baicaizhale.litemotto;

public class CloudflareAccountInfo {
    private final String accountId;
    private final String apiKey;

    public CloudflareAccountInfo(String accountId, String apiKey) {
        this.accountId = accountId;
        this.apiKey = apiKey;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getApiKey() {
        return apiKey;
    }
}
