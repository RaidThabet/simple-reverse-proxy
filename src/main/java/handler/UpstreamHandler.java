package handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpstreamHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final Channel channel;

    public UpstreamHandler(Channel channel) {
        this.channel = channel;
    }

    private static final Logger log = LoggerFactory.getLogger(UpstreamHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        channel.writeAndFlush(msg.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error occured: ", cause);
        ctx.close();
    }
}
