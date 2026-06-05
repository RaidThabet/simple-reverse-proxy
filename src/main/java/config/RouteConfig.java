package config;

public class RouteConfig {
    private String prefix;

    private UpstreamConfig upstream;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public UpstreamConfig getUpstream() {
        return upstream;
    }

    public void setUpstream(UpstreamConfig upstream) {
        this.upstream = upstream;
    }
}
