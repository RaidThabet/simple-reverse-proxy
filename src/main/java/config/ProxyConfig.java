package config;

import java.util.List;

public class ProxyConfig {

    private ServerConfig server;

    private List<RouteConfig> routes;

    public List<RouteConfig> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteConfig> routes) {
        this.routes = routes;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }
}



