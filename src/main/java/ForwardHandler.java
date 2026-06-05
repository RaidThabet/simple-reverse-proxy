import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ForwardHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(ForwardHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        String path = decoder.path();

        // current routes are hardcoded for initial test purposes
        // TODO: configure routes in a YAML file
        if (path.startsWith("/api/order")) {
            // forward the request to order rest api
            String host = "localhost";
            int port = 8081;
            String apiPath = path.substring(10);
            if (apiPath.isEmpty()) apiPath = "/";
            forwardRequest(host, port, apiPath, req, ctx);
            return;
        }

        if (path.startsWith("/api/product")) {
            // forward the request to product rest api
            String host = "localhost";
            int port = 8082;
            String apiPath = path.substring(12);
            if (apiPath.isEmpty()) apiPath = "/";
            forwardRequest(host, port, apiPath, req, ctx);
            return;
        }

        // handle bad gateway
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY
        );
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
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
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new HttpClientCodec(),
                                new HttpObjectAggregator(65536),
                                new UpstreamHandler(ctx.channel())
                        );
                    }
                });
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
