import config.ProxyConfig;
import initializer.ReverseProxyServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ConfigLoader;

import java.io.IOException;

public class ReverseProxyServer {

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyServer.class);

    static void main() throws InterruptedException, IOException {

        ProxyConfig config = ConfigLoader.load("config.yaml");

        int port = config.getServer().getPort();

        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(group)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ReverseProxyServerChannelInitializer());

            ChannelFuture bindFuture = bootstrap.bind(port).sync();
            log.info("Server listening on port 8080");
            bindFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
