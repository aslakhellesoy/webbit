package org.webbitserver.mongrel2;

import org.webbitserver.HttpRequest;
import org.webbitserver.mongrel2.contrib.Request;

import java.net.HttpCookie;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Mongrel2Request implements HttpRequest {
    private final Request request;

    public Mongrel2Request(Request request) {
        this.request = request;
    }

    @Override
    public String uri() {
        return request.uri().toString();
    }

    @Override
    public HttpRequest uri(String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String header(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> headers(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasHeader(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<HttpCookie> cookies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpCookie cookie(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String queryParam(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> queryParams(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> queryParamKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String postParam(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> postParams(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> postParamKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String cookieValue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Map.Entry<String, String>> allHeaders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String method() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String body() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> data() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object data(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpRequest data(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> dataKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress remoteAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object id() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long timestamp() {
        throw new UnsupportedOperationException();
    }
}
