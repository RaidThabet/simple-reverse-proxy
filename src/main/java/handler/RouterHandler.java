package handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import util.Route;

import java.util.List;

public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final AttributeKey<Route> ROUTE_KEY = AttributeKey.valueOf("route");
    public static final AttributeKey<String> REWRITTEN_URI_KEY = AttributeKey.valueOf("rewrittenUri");

    // current routes are hardcoded for initial test purposes
    // TODO: configure routes in a YAML config file
    private static final List<Route> ROUTES = List.of(
            new Route("/api/order", "localhost", 8081),
            new Route("/api/product", "localhost", 8082)
    );

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        req.retain();

        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        String path = decoder.path();

        ROUTES.stream()
                .filter(r -> path.startsWith(r.prefix()))
                .findFirst()
                .ifPresentOrElse(route -> {
                            String apiPath = path.substring(route.prefix().length());
                            if (apiPath.isEmpty()) apiPath = "/";

                            // store on channel for the forward handler
                            ctx.channel().attr(ROUTE_KEY).set(route);
                            ctx.channel().attr(REWRITTEN_URI_KEY).set(apiPath);

                            ctx.fireChannelRead(req);
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
}
