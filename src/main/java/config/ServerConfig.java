package config;

public class ServerConfig {

    private int port;

    private RateLimitConfig ratelimit;

    public RateLimitConfig getRatelimit() {
        return ratelimit;
    }

    public void setRatelimit(RateLimitConfig ratelimit) {
        this.ratelimit = ratelimit;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
