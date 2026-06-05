package config;

public class RateLimitConfig {

    private int maxTokens;

    private int refillRatePerSecond;

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getRefillRatePerSecond() {
        return refillRatePerSecond;
    }

    public void setRefillRatePerSecond(int refillRatePerSecond) {
        this.refillRatePerSecond = refillRatePerSecond;
    }
}
