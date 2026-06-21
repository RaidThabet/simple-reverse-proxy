package handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutException;
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
        channel.writeAndFlush(msg.retain()).addListener(f -> ctx.close());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            log.error("Upstream read timed out", cause);
            channel.writeAndFlush(new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.GATEWAY_TIMEOUT
            )).addListener(ChannelFutureListener.CLOSE);
        } else {
            log.error("Error occured: ", cause);
        }
        ctx.close();
    }
}
