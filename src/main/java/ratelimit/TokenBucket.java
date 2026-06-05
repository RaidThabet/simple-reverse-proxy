package ratelimit;

public class TokenBucket {

    private final int maxTokens;

    private final double refillRate; // in ms

    private double currentTokens;

    private long lastRefillTime;

    public TokenBucket(int maxTokens, double refillRatePerSecond) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRatePerSecond / 1000.0;
        this.currentTokens = maxTokens;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (currentTokens >= 1) {
            currentTokens--;
            return true; // allowed
        }
        return false; // rate limited
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        currentTokens = Math.min(maxTokens, currentTokens + elapsed * refillRate);
        lastRefillTime = now;
    }

    public double getCurrentTokens() {
        return currentTokens;
    }
}
