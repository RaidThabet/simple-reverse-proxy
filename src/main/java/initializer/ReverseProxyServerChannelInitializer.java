package initializer;

import handler.ForwardHandler;
import handler.RouterHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import util.ConfigLoader;
import util.Route;

import java.util.List;

public class ReverseProxyServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final List<Route> routes = ConfigLoader.get().getRoutes().stream()
            .map(r -> new Route(
                    r.getPrefix(),
                    r.getUpstream().getHost(),
                    r.getUpstream().getPort()
            ))
            .toList();

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                new HttpServerCodec(),
                new HttpObjectAggregator(65536),
                new RouterHandler(routes),
                new ForwardHandler()
        );
    }
}
