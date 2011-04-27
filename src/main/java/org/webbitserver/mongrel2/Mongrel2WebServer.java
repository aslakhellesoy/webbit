package org.webbitserver.mongrel2;

import org.webbitserver.*;
import org.webbitserver.handler.HttpToEventSourceHandler;
import org.webbitserver.handler.PathMatchHandler;
import org.webbitserver.handler.exceptions.PrintStackTraceExceptionHandler;
import org.webbitserver.handler.exceptions.SilentExceptionHandler;
import org.webbitserver.mongrel2.contrib.Connection;
import org.webbitserver.mongrel2.contrib.Request;
import org.webbitserver.mongrel2.contrib.Response;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Mongrel2WebServer implements WebServer {
    // https://github.com/tupshin/Mongrel2-Java/tree/master/src/com/cthulupus/mongrel2
    // http://www.paperculture.com/code/Chat.java
    // http://ncampion.posterous.com/mongrel-2-and-java-handler
    private static final String SENDER_ID = "049f77fe-646d-11e0-901e-bf9ab42ab7ab";
    private static final String SUB_ADDRESS = "tcp://127.0.0.1:9999";
    private static final String PUB_ADDRESS = "tcp://127.0.0.1:9998";

    private final List<HttpHandler> handlers = new ArrayList<HttpHandler>();

    private final Executor executor;
    private final Connection connection;
    private Executor mongrelExecutor;

    private Thread.UncaughtExceptionHandler exceptionHandler;
    private Thread.UncaughtExceptionHandler ioExceptionHandler;
    
    public Mongrel2WebServer() {
        this(Executors.newSingleThreadScheduledExecutor());
    }

    public Mongrel2WebServer(final Executor executor) {
        this(executor, new Connection(SENDER_ID, SUB_ADDRESS, PUB_ADDRESS));
    }

    public Mongrel2WebServer(final Executor executor, Connection connection) {
        this.executor = executor;
        this.connection = connection;

        // Uncaught exceptions from handlers get dumped to console by default.
        // To change, call uncaughtExceptionHandler()
        uncaughtExceptionHandler(new PrintStackTraceExceptionHandler());

        // Default behavior is to silently discard any exceptions caused
        // when reading/writing to the client. The Internet is flaky - it happens.
        connectionExceptionHandler(new SilentExceptionHandler());
    }

    @Override
    public Mongrel2WebServer add(HttpHandler handler) {
        handlers.add(handler);
        return this;
    }

    @Override
    public Mongrel2WebServer add(String path, HttpHandler handler) {
        return add(new PathMatchHandler(path, handler));
    }

    @Override
    public Mongrel2WebServer add(String path, WebSocketHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mongrel2WebServer add(String path, EventSourceHandler handler) {
        return add(path, new HttpToEventSourceHandler(handler));
    }

    @Override
    public WebServer start() throws IOException {
        mongrelExecutor = Executors.newSingleThreadExecutor();
        mongrelExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        handle(connection.recv());
                    } catch (Exception e) {
                        exceptionHandler.uncaughtException(Thread.currentThread(), e);
                    }
                }
            }
        });
        return this;
    }

    private void handle(Request request) throws Exception {
        final Mongrel2Request mongrel2Request = new Mongrel2Request(request);
        final Mongrel2Response mongrel2Response = new Mongrel2Response(new Response(request, connection));
        final HttpControl control = new Mongrel2HttpControl(handlers.iterator(), executor, mongrel2Request, mongrel2Response);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    control.nextHandler(mongrel2Request, mongrel2Response);
                } catch (Exception exception) {
                    exceptionHandler.uncaughtException(Thread.currentThread(), exception);
                }
            }
        });
    }

    @Override
    public Mongrel2WebServer stop() throws IOException {
        return this;
    }

    @Override
    public Mongrel2WebServer join() throws InterruptedException {
        return this;
    }

    @Override
    public Mongrel2WebServer uncaughtExceptionHandler(Thread.UncaughtExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    @Override
    public Mongrel2WebServer connectionExceptionHandler(Thread.UncaughtExceptionHandler ioExceptionHandler) {
        this.ioExceptionHandler = ioExceptionHandler;
        return this;
    }

    @Override
    public URI getUri() {
        // TODO: Make this configurable when we start mongrel2 in start
        return URI.create("http://localhost:6767");
    }

    @Override
    public Executor getExecutor() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) throws IOException {
        new Mongrel2WebServer()
                .add(new HttpHandler() {
                    @Override
                    public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
                        System.out.println("request = " + request.uri());
                        response.content("Hello").end();
                    }
                })
                .start();
    }
}
