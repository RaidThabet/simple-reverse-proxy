package handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import util.Route;

import java.util.List;

public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final List<Route> routes;

    public static final AttributeKey<Route> ROUTE_KEY = AttributeKey.valueOf("route");
    public static final AttributeKey<String> REWRITTEN_URI_KEY = AttributeKey.valueOf("rewrittenUri");

    public RouterHandler(List<Route> routes) {
        this.routes = routes;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        req.retain();

        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        String path = decoder.path();

        routes.stream()
                .filter(r -> path.startsWith(r.prefix()))
                .findFirst()
                .ifPresentOrElse(route -> {
                            String apiPath = path.substring(route.prefix().length());
                            if (apiPath.isEmpty()) apiPath = "/";

                            // store on channel for the forward handler to read
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
