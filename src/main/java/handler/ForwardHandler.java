package handler;

import initializer.UpstreamChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
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
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
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

                // add the client ip in the X-Forwarded-For header
                String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
                forwardRequest.headers().set("X-Forwarded-For", clientIp);

                future.channel().writeAndFlush(forwardRequest);

                // if client disconnects before upstream responds, close upstream channel
                ctx.channel().closeFuture().addListener(f -> future.channel().close());
            } else {
                // handle bad gateway
                future.channel().close(); // close upstream connection
                ctx.writeAndFlush(new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY
                        )
                ).addListener(ChannelFutureListener.CLOSE); // close client connection
            }
            req.release();
        });
    }
}
