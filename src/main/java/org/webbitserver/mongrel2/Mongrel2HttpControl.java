package org.webbitserver.mongrel2;

import org.webbitserver.*;

import java.util.Iterator;
import java.util.concurrent.Executor;

public class Mongrel2HttpControl implements HttpControl {
    private final Iterator<HttpHandler> handlerIterator;
    private final Executor executor;

    private HttpRequest defaultRequest;
    private HttpResponse defaultResponse;
    private HttpControl defaultControl;

    public Mongrel2HttpControl(Iterator<HttpHandler> handlerIterator, Executor executor, Mongrel2Request mongrel2Request, Mongrel2Response mongrel2Response) {
        this.handlerIterator = handlerIterator;
        this.executor = executor;
        
        defaultRequest = mongrel2Request;
        defaultResponse = mongrel2Response;
        defaultControl = this;
    }

    // Dupe
    @Override
    public void nextHandler() {
        nextHandler(defaultRequest, defaultResponse, defaultControl);
    }

    // Dupe
    @Override
    public void nextHandler(HttpRequest request, HttpResponse response) {
        nextHandler(request, response, defaultControl);
    }

    @Override
    public void nextHandler(HttpRequest request, HttpResponse response, HttpControl control) {
        this.defaultRequest = request;
        this.defaultResponse = response;
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
    public WebSocketConnection upgradeToWebSocketConnection(WebSocketHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebSocketConnection createWebSocketConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventSourceConnection upgradeToEventSourceConnection(EventSourceHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventSourceConnection createEventSourceConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Executor handlerExecutor() {
        return executor;
    }

    @Override
    public void execute(Runnable command) {
        handlerExecutor().execute(command);
    }
}
