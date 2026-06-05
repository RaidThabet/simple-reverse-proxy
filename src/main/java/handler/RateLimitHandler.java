package handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import ratelimit.TokenBucket;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitHandler extends ChannelInboundHandlerAdapter {

    private static final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final int maxTokens;
    private final int refillRatePerSecond;

    public RateLimitHandler(int maxTokens, int refillRatePerSecond) {
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress())
                .getAddress().getHostAddress();

        TokenBucket bucket = buckets.computeIfAbsent(
                clientIp,
                ip -> new TokenBucket(maxTokens, refillRatePerSecond)
        );

        if (bucket.tryConsume()) {
            ctx.fireChannelRead(msg); // allowed -> pass to RouterHandler
        } else {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.TOO_MANY_REQUESTS
            );
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(response);
        }
    }
}
