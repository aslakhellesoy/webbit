package org.webbitserver.mongrel2.contrib;

import java.util.HashMap;
import java.util.Map;

public class Response {
    private final Request request;
    private final Connection connection;
    private final Map<String, String> headers = new HashMap<String, String>();

    private int status = 200;
    private byte[] content;

    public Response(Request req, Connection connection) {
        this.request = req;
        this.connection = connection;
    }

    public void end() {
        connection.replyHttp(request, content, status, "OK", headers);
    }

    public void content(byte[] content) {
        this.content = content;
    }
}
