package initializer;

import handler.UpstreamHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

public class UpstreamChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Channel channel;

    public UpstreamChannelInitializer(Channel channel) {
        this.channel = channel;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                new ReadTimeoutHandler(10, TimeUnit.SECONDS),
                new HttpClientCodec(),
                new HttpObjectAggregator(65536),
                new UpstreamHandler(channel)
        );
    }
}
