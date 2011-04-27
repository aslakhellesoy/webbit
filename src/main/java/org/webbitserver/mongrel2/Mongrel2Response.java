package org.webbitserver.mongrel2;

import org.webbitserver.HttpResponse;
import org.webbitserver.mongrel2.contrib.Response;

import java.net.HttpCookie;
import java.nio.charset.Charset;

public class Mongrel2Response implements HttpResponse {
    private final Response response;

    public Mongrel2Response(Response response) {
        this.response = response;
    }

    @Override
    public HttpResponse charset(Charset charset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Charset charset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse status(int status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int status() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse header(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse header(String name, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse cookie(HttpCookie httpCookie) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse content(String content) {
        return this;
    }

    @Override
    public HttpResponse write(String content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse content(byte[] content) {
        response.content(content);
        return this;
    }

    @Override
    public HttpResponse error(Throwable error) {
        error.printStackTrace();
        return this;
    }

    @Override
    public HttpResponse end() {
        response.end();
        return this;
    }
}
