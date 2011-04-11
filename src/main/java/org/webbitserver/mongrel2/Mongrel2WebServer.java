package org.webbitserver.mongrel2;

import org.webbitserver.EventSourceHandler;
import org.webbitserver.HttpHandler;
import org.webbitserver.WebServer;
import org.webbitserver.WebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executor;

public class Mongrel2WebServer implements WebServer {
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
}
