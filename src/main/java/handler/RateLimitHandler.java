package handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratelimit.TokenBucket;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class RateLimitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitHandler.class);

    private static final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

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

        TokenBucket bucket = buckets.get(
                clientIp,
                ip -> new TokenBucket(maxTokens, refillRatePerSecond)
        );

        if (bucket.tryConsume()) {
            ctx.fireChannelRead(msg); // allowed -> pass to RouterHandler
        } else {
            ReferenceCountUtil.release(msg);
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.TOO_MANY_REQUESTS
            );
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("RateLimitHandler error: ", cause);
        ctx.writeAndFlush(new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR
        )).addListener(ChannelFutureListener.CLOSE);
    }
}
