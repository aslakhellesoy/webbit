package org.webbitserver.netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.WebSocketHandler;
import org.webbitserver.WebbitException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyHttpChannelHandler extends SimpleChannelUpstreamHandler {

    private final Executor executor;
    private final List<HttpHandler> httpHandlers;
    private final List<WebSocketHandler> webSocketHandlers;
    private final Object id;
    private final long timestamp;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final Thread.UncaughtExceptionHandler ioExceptionHandler;
    private final ConnectionHelper connectionHelper;
    private final Map<ChannelHandlerContext,NettyWebSocketConnection> connections = new HashMap<ChannelHandlerContext, NettyWebSocketConnection>();

    public NettyHttpChannelHandler(Executor executor,
                                   List<HttpHandler> httpHandlers,
                                   List<WebSocketHandler> webSocketHandlers,
                                   Object id,
                                   long timestamp,
                                   Thread.UncaughtExceptionHandler exceptionHandler,
                                   Thread.UncaughtExceptionHandler ioExceptionHandler) {
        this.executor = executor;
        this.httpHandlers = httpHandlers;
        this.webSocketHandlers = webSocketHandlers;
        this.id = id;
        this.timestamp = timestamp;
        this.exceptionHandler = exceptionHandler;
        this.ioExceptionHandler = ioExceptionHandler;

        connectionHelper = new ConnectionHelper(executor, exceptionHandler, ioExceptionHandler) {
            @Override
            protected void fireOnClose() throws Exception {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent messageEvent) throws Exception {
        Object msg = messageEvent.getMessage();
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, messageEvent, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else {
            super.messageReceived(ctx, messageEvent);
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, final WebSocketFrame webSocketFrame) {
        final NettyWebSocketConnection webSocketConnection = connections.get(ctx);
        for (final WebSocketHandler webSocketHandler : webSocketHandlers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (webSocketFrame instanceof TextWebSocketFrame) {
                        TextWebSocketFrame frame = (TextWebSocketFrame) webSocketFrame;
                        try {
                            webSocketHandler.onMessage(webSocketConnection, frame.getText());
                        } catch (Throwable throwable) {
                            exceptionHandler.uncaughtException(Thread.currentThread(), throwable);
                        }
                    }
                }
            });
        }
    }

    private void handleHttpRequest(final ChannelHandlerContext ctx, MessageEvent messageEvent, HttpRequest httpRequest) {
        final NettyHttpRequest nettyHttpRequest = new NettyHttpRequest(
                messageEvent,
                httpRequest,
                id,
                timestamp
        );
        final NettyHttpResponse nettyHttpResponse = new NettyHttpResponse(
                ctx,
                new DefaultHttpResponse(HTTP_1_1, OK),
                isKeepAlive(httpRequest),
                exceptionHandler
        );
        final HttpControl control = new NettyHttpControl(
                httpHandlers.iterator(),
                executor,
                ctx,
                nettyHttpRequest,
                nettyHttpResponse,
                httpRequest,
                new DefaultHttpResponse(HTTP_1_1, OK),
                exceptionHandler,
                ioExceptionHandler,
                this
        );

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    control.nextHandler(nettyHttpRequest, nettyHttpResponse);
                } catch (Exception exception) {
                    exceptionHandler.uncaughtException(Thread.currentThread(), WebbitException.fromException(exception, ctx.getChannel()));
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, final ExceptionEvent e) {
        connectionHelper.fireConnectionException(e);
    }

    public void registerWebSocketConnection(ChannelHandlerContext ctx, NettyWebSocketConnection webSocketConnection) {
        connections.put(ctx, webSocketConnection);
    }
}
