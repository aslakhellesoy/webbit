package org.webbitserver.mongrel2;

import com.paperculture.mongrel2.Handler;
import org.webbitserver.EventSourceHandler;
import org.webbitserver.HttpHandler;
import org.webbitserver.WebServer;
import org.webbitserver.WebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executor;

public class Mongrel2WebServer implements WebServer {
    // https://github.com/tupshin/Mongrel2-Java/tree/master/src/com/cthulupus/mongrel2
    // http://www.paperculture.com/code/Chat.java
    // http://ncampion.posterous.com/mongrel-2-and-java-handler
    private static final String SENDER_ID = "049f77fe-646d-11e0-901e-bf9ab42ab7ab";
    private static final String SUB_ADDRESS = "tcp://127.0.0.1:9999";
    private static final String PUB_ADDRESS = "tcp://127.0.0.1:9998";
    
    private Handler.Connection conn;

    public Mongrel2WebServer() {
        conn = new Handler.Connection(SENDER_ID, SUB_ADDRESS, PUB_ADDRESS);
        Handler.Request request = conn.recv();
        System.out.println("request = " + request);
    }
    
    @Override
    public WebServer add(HttpHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebServer add(String path, HttpHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebServer add(String path, WebSocketHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebServer add(String path, EventSourceHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebServer start() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebServer stop() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebServer join() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebServer uncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebServer connectionExceptionHandler(Thread.UncaughtExceptionHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI getUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Executor getExecutor() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) {
        new Mongrel2WebServer();
    }
}
