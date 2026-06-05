package handler;

import initializer.UpstreamChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Route;

import java.net.InetSocketAddress;

public class ForwardHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(ForwardHandler.class);


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        Route route = ctx.channel().attr(RouterHandler.ROUTE_KEY).get();
        String apiPath = ctx.channel().attr(RouterHandler.REWRITTEN_URI_KEY).get();

        forwardRequest(route.host(), route.port(), apiPath, req, ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().remoteAddress());
    }

    private void forwardRequest(String host, int port, String apiPath, FullHttpRequest req, ChannelHandlerContext ctx) {
        req.retain();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new UpstreamChannelInitializer(ctx.channel()));

        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            // if the target api connected successfully, send the request. Else, return BAD_GATEWAY HTTP status
            // to the original client
            if (future.isSuccess()) {
                // the original request object is immutable, so we need to copy it and
                // set the new uri
                DefaultFullHttpRequest forwardRequest = new DefaultFullHttpRequest(
                        req.protocolVersion(),
                        req.method(),
                        apiPath,
                        req.content().retain()
                );

                // copy headers
                forwardRequest.headers().set(req.headers());
                forwardRequest.headers().set(HttpHeaderNames.HOST, host + ":" + port);
                String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
                forwardRequest.headers().set("X-Forwarded-For", clientIp);

                future.channel().writeAndFlush(forwardRequest);
            } else {
                // handle bad gateway
                ctx.writeAndFlush(new DefaultHttpResponse(
                                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY
                        )
                );
            }
            req.release();
        });
    }
}
