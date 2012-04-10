package org.webbitserver.netty;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.webbitserver.EventSourceHandler;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.WebSocketHandler;
import org.webbitserver.WebbitException;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class NettyHttpControl implements HttpControl {

    private final Iterator<HttpHandler> handlerIterator;
    private final Executor executor;
    private final ChannelHandlerContext ctx;
    private final NettyHttpRequest webbitHttpRequest;
    private final org.jboss.netty.handler.codec.http.HttpRequest nettyHttpRequest;
    private final org.jboss.netty.handler.codec.http.HttpResponse nettyHttpResponse;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final Thread.UncaughtExceptionHandler ioExceptionHandler;
    private final NettyHttpChannelHandler nettyHttpChannelHandler;

    private HttpRequest defaultRequest;
    private HttpResponse webbitHttpResponse;
    private HttpControl defaultControl;
    private NettyWebSocketConnection webSocketConnection;
    private NettyEventSourceConnection eventSourceConnection;

    public NettyHttpControl(Iterator<HttpHandler> handlerIterator,
                            Executor executor,
                            ChannelHandlerContext ctx,
                            NettyHttpRequest webbitHttpRequest,
                            NettyHttpResponse webbitHttpResponse,
                            org.jboss.netty.handler.codec.http.HttpRequest nettyHttpRequest,
                            org.jboss.netty.handler.codec.http.HttpResponse nettyHttpResponse,
                            Thread.UncaughtExceptionHandler exceptionHandler,
                            Thread.UncaughtExceptionHandler ioExceptionHandler,
                            NettyHttpChannelHandler nettyHttpChannelHandler
    ) {
        this.handlerIterator = handlerIterator;
        this.executor = executor;
        this.ctx = ctx;
        this.webbitHttpRequest = webbitHttpRequest;
        this.webbitHttpResponse = webbitHttpResponse;
        this.nettyHttpRequest = nettyHttpRequest;
        this.nettyHttpResponse = nettyHttpResponse;
        this.ioExceptionHandler = ioExceptionHandler;
        this.exceptionHandler = exceptionHandler;

        defaultRequest = webbitHttpRequest;
        this.nettyHttpChannelHandler = nettyHttpChannelHandler;
        defaultControl = this;
    }

    @Override
    public void nextHandler() {
        nextHandler(defaultRequest, webbitHttpResponse, defaultControl);
    }

    @Override
    public void nextHandler(HttpRequest request, HttpResponse response) {
        nextHandler(request, response, defaultControl);
    }

    @Override
    public void nextHandler(HttpRequest request, HttpResponse response, HttpControl control) {
        this.defaultRequest = request;
        this.webbitHttpResponse = response;
        this.defaultControl = control;
        if (handlerIterator.hasNext()) {
            HttpHandler handler = handlerIterator.next();
            try {
                handler.handleHttpRequest(request, response, control);
            } catch (Throwable e) {
                response.error(e);
            }
        } else {
            response.status(404).end();
        }
    }

    @Override
    public WebSocketConnection performWebSocketHandshake(final WebSocketHandler webSocketHandler) {
        final NettyWebSocketConnection webSocketConnection = webSocketConnection();

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(), null, false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(nettyHttpRequest);
        if (handshaker == null) {
            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        } else {
            final ChannelFuture handshake = handshaker.handshake(ctx.getChannel(), nettyHttpRequest);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean completedHandshake = handshake.await(3000, TimeUnit.MILLISECONDS);
                        if (completedHandshake) {
                            try {
                                nettyHttpChannelHandler.registerWebSocketConnection(ctx, webSocketConnection);
                                webSocketHandler.onOpen(webSocketConnection);
                            } catch (Throwable e) {
                                exceptionHandler.uncaughtException(Thread.currentThread(), new WebbitException(e));
                            }
                        } else {
                            try {
                                webSocketHandler.onClose(webSocketConnection);
                            } catch (Throwable e) {
                                exceptionHandler.uncaughtException(Thread.currentThread(), new WebbitException(e));
                            }
                        }
                    } catch (InterruptedException e) {
                        try {
                            webSocketHandler.onClose(webSocketConnection);
                        } catch (Throwable throwable) {
                            exceptionHandler.uncaughtException(Thread.currentThread(), new WebbitException(e));
                        }
                    }
                }
            });
        }
        return webSocketConnection;
    }

    private String getWebSocketLocation() {
        return "ws://" + nettyHttpRequest.getHeader(HttpHeaders.Names.HOST) + "/foooo";
    }

    @Override
    public NettyWebSocketConnection webSocketConnection() {
        if (webSocketConnection == null) {
            webSocketConnection = new NettyWebSocketConnection(executor, webbitHttpRequest, ctx);
        }
        return webSocketConnection;
    }

    @Override
    public NettyEventSourceConnection upgradeToEventSourceConnection(EventSourceHandler eventSourceHandler) {
        NettyEventSourceConnection eventSourceConnection = eventSourceConnection();
        EventSourceConnectionHandler eventSourceConnectionHandler = new EventSourceConnectionHandler(executor, exceptionHandler, ioExceptionHandler, eventSourceConnection, eventSourceHandler);
        performEventSourceHandshake(eventSourceConnectionHandler);

        try {
            eventSourceHandler.onOpen(eventSourceConnection);
        } catch (Exception e) {
            exceptionHandler.uncaughtException(Thread.currentThread(), new WebbitException(e));
        }
        return eventSourceConnection;
    }

    @Override
    public NettyEventSourceConnection eventSourceConnection() {
        if (eventSourceConnection == null) {
            eventSourceConnection = new NettyEventSourceConnection(executor, webbitHttpRequest, ctx);
        }
        return eventSourceConnection;
    }

    @Override
    public Executor handlerExecutor() {
        return executor;
    }

    @Override
    public void execute(Runnable command) {
        handlerExecutor().execute(command);
    }

    private void performEventSourceHandshake(ChannelHandler eventSourceConnectionHandler) {
        nettyHttpResponse.setStatus(HttpResponseStatus.OK);
        nettyHttpResponse.addHeader("Content-Type", "text/event-stream");
        nettyHttpResponse.addHeader("Transfer-Encoding", "identity");
        nettyHttpResponse.addHeader("Connection", "keep-alive");
        nettyHttpResponse.addHeader("Cache-Control", "no-cache");
        nettyHttpResponse.setChunked(false);
        ctx.getChannel().write(nettyHttpResponse);
        getReadyToSendEventSourceMessages(eventSourceConnectionHandler);
    }

    private void getReadyToSendEventSourceMessages(ChannelHandler eventSourceConnectionHandler) {
        ChannelPipeline p = ctx.getChannel().getPipeline();
        StaleConnectionTrackingHandler staleConnectionTracker = (StaleConnectionTrackingHandler) p.remove("staleconnectiontracker");
        staleConnectionTracker.stopTracking(ctx.getChannel());
        p.remove("aggregator");
        p.replace("handler", "ssehandler", eventSourceConnectionHandler);
    }

}
