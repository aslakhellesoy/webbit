package org.webbitserver.netty;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.webbitserver.WebSocketConnection;

import java.util.concurrent.Executor;

public class NettyWebSocketConnection extends AbstractHttpConnection implements WebSocketConnection {

    private String version;

    public NettyWebSocketConnection(Executor executor, NettyHttpRequest nettyHttpRequest, ChannelHandlerContext ctx) {
        super(ctx, nettyHttpRequest, executor);
    }

    @Override
    public NettyWebSocketConnection send(String message) {
        writeMessage(new TextWebSocketFrame(message));
        return this;
    }

    @Override
    public NettyWebSocketConnection send(byte[] message) {
        return send(message, 0, message.length);
    }

    @Override
    public NettyWebSocketConnection send(byte[] message, int offset, int length) {
        writeMessage(new BinaryWebSocketFrame(ChannelBuffers.copiedBuffer(message, offset, length)));
        return this;
    }

    @Override
    public NettyWebSocketConnection ping(byte[] message) {
        writeMessage(new PingWebSocketFrame(ChannelBuffers.copiedBuffer(message)));
        return this;
    }

    @Override
    public NettyWebSocketConnection pong(byte[] message) {
        writeMessage(new PongWebSocketFrame(ChannelBuffers.copiedBuffer(message)));
        return this;
    }

    @Override
    public NettyWebSocketConnection close() {
        closeChannel();
        return this;
    }

    @Override
    public NettyWebSocketConnection data(String key, Object value) {
        putData(key, value);
        return this;
    }

    @Override
    public String version() {
        return version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    public void setHybiWebSocketVersion(int webSocketVersion) {
        setVersion("Sec-WebSocket-Version-" + webSocketVersion);
    }
}
