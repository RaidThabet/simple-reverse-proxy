package handler;

import initializer.UpstreamChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Route;

import java.net.InetSocketAddress;
import java.util.List;

public class ForwardHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(ForwardHandler.class);

    private static final List<Route> ROUTES = List.of(
            new Route("/api/order", "localhost", 8081),
            new Route("/api/product", "localhost", 8082)
    );

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        String path = decoder.path();

        // current routes are hardcoded for initial test purposes
        // TODO: configure routes in a YAML file
        ROUTES.stream()
                .filter(r -> path.startsWith(r.prefix()))
                .findFirst()
                .ifPresentOrElse(route -> {
                            String apiPath = path.substring(route.prefix().length());
                            if (apiPath.isEmpty()) apiPath = "/";
                            forwardRequest(route.host(), route.port(), apiPath, req, ctx);
                        },
                        // handle bad gateway
                        () -> {
                            FullHttpResponse response = new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY
                            );
                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        }
                );

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
                ctx.writeAndFlush(new DefaultHttpResponse(
                                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY
                        )
                );
            }
            req.release();
        });
    }
}
