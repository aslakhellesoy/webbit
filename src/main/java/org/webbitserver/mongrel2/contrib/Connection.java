package org.webbitserver.mongrel2.contrib;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class Connection {
    /**
     * One context per jvm; see: http://zguide.zeromq.org/chapter:all#toc10
     */
    private static final ZMQ.Context CTX = ZMQ.context(1);
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private static final Map<String,String> EMPTY_HEADERS = Collections.<String, String>emptyMap();

    private final byte[] senderId;
    private final String subAddress, pubAddress;
    private final ZMQ.Socket reqs, resp;

    public Connection(String senderId, String subAddress, String pubAddress) {
        this(senderId.getBytes(ASCII), subAddress, pubAddress);
    }

    private Connection(byte[] senderId, String subAddress, String pubAddress) {
        this.senderId = senderId;
        this.subAddress = subAddress;
        this.pubAddress = pubAddress;
        this.reqs = CTX.socket(ZMQ.PULL);
        reqs.connect(subAddress);
        this.resp = CTX.socket(ZMQ.PUB);
        resp.connect(pubAddress);
        resp.setIdentity(senderId);
    }

    public Request recv() throws IOException {
        return Request.parse(reqs.recv(0));
    }


    private void send(String uuid, String connId, byte[] msg) {
        final byte[] checkedMsg = msg == null ? EMPTY_BYTE_ARRAY : msg;
        final byte[] header = (uuid + ' ' + connId.length() + ':' + connId + ", ")
                .getBytes(ASCII);
        resp.send(concat(header, checkedMsg), 0);
    }

    public void reply(Request req, byte[] msg) {
        send(req.uuid, req.id, msg);
    }

    public void reply(Request req, String msg) {
        send(req.uuid, req.id, msg.getBytes(ASCII));
    }

    public void replyHttp(Request req, String body) {
        replyHttp(req, body, 200);
    }

    public void replyHttp(Request req, String body, int code) {
        replyHttp(req, body, code, "OK");
    }

    public void replyHttp(Request req, String body, int code, String status) {
        replyHttp(req, body, code, status, EMPTY_HEADERS);
    }

    public void replyHttp(Request req, String body, int code, String status,
                          Map<String, String> headers) {
        replyHttp(req, body.getBytes(ASCII), code, status, headers);
    }

    public void replyHttp(Request req, byte[] body) {
        replyHttp(req, body, 200);
    }

    public void replyHttp(Request req, byte[] body, int code) {
        replyHttp(req, body, code, "OK");
    }

    public void replyHttp(Request req, byte[] body, int code, String status) {
        replyHttp(req, body, code, status, EMPTY_HEADERS);
    }

    public void replyHttp(Request req, byte[] body, int code, String status,
                          Map<String, String> headers) {
        reply(req, httpResponse(body, code, status, headers));
    }

    /**
     * This lets you send a single message to many currently connected clients.
     * There's a MAX_IDENTS that you should not exceed, so chunk your targets as needed.
     * Each target will receive the message once by Mongrel2, but you don't have
     * to loop which cuts down on reply volume.
     */
    public void deliver(String uuid, Iterable<String> connIds, byte[] msg) {
        send(uuid, join(connIds, " "), msg);
    }

    public void deliver(String uuid, Iterable<String> connIds, String msg) {
        send(uuid, join(connIds, " "), msg.getBytes(ASCII));
    }

//    /** Same as {@link Connection#deliver(String, Iterable, byte[])}, but converts to JSON first. */
//    public void deliverJson(String uuid, Iterable<String> connIds, Map<String, Object> jsonData) {
//      deliver(uuid, connIds, new JSONObject(jsonData).toString().getBytes(ASCII));
//    }

    /**
     * Same as deliver, but builds an HTTP response, which means, yes, you can
     * reply to multiple connected clients waiting for an HTTP response from one
     * handler. Kinda cool.
     */
    public void deliverHttp(String uuid, Iterable<String> connIds, String body) {
        deliverHttp(uuid, connIds, body, 200);
    }

    public void deliverHttp(String uuid, Iterable<String> connIds, String body, int code) {
        deliverHttp(uuid, connIds, body, code, "OK");
    }

    public void deliverHttp(String uuid, Iterable<String> connIds, String body,
                            int code, String status) {
        deliverHttp(uuid, connIds, body, code, status, EMPTY_HEADERS);
    }

    public void deliverHttp(String uuid, Iterable<String> connIds, String body,
                            int code, String status, Map<String, String> headers) {
        final byte[] checkedBody = body == null ? EMPTY_BYTE_ARRAY : body.getBytes(ASCII);
        deliverHttp(uuid, connIds, checkedBody, code, status, headers);
    }

    public void deliverHttp(String uuid, Iterable<String> connIds, byte[] body) {
        deliverHttp(uuid, connIds, body, 200);
    }

    public void deliverHttp(String uuid, Iterable<String> connIds, byte[] body, int code) {
        deliverHttp(uuid, connIds, body, code, "OK");
    }

    public void deliverHttp(String uuid, Iterable<String> connIds, byte[] body,
                            int code, String status) {
        deliverHttp(uuid, connIds, body, code, status, EMPTY_HEADERS);
    }

    public void deliverHttp(String uuid, Iterable<String> connIds, byte[] body,
                            int code, String status, Map<String, String> headers) {
        deliver(uuid, connIds, httpResponse(body, code, status, headers));
    }

    /**
     * Tells mongrel2 to explicitly close the HTTP connection.
     */
    public void close(Request req) {
        reply(req, EMPTY_BYTE_ARRAY);
    }

    /**
     * Same as close but does it to a whole bunch of idents at a time.
     */
    public void deliverClose(String uuid, Iterable<String> connIds) {
        deliver(uuid, connIds, EMPTY_BYTE_ARRAY);
    }

    public byte[] getSenderId() {
        return Arrays.copyOf(senderId, senderId.length);
    }

    public String getSenderIdString() {
        return new String(senderId, ASCII);
    }

    public String getSubAddress() {
        return subAddress;
    }

    public String getPubAddress() {
        return pubAddress;
    }

    @Override
    public String toString() {
        return "Connection [senderId=" + Arrays.toString(senderId) + ", subAddress="
                + subAddress + ", pubAddress=" + pubAddress + "]";
    }

    @Override
    public int hashCode() {
        return hash(senderId, pubAddress, subAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Connection)) return false;
        final Connection that = (Connection) o;
        return Arrays.equals(senderId, that.senderId) && eq(pubAddress, that.pubAddress)
                && eq(subAddress, that.subAddress);
    }

    private static String join(Iterable<?> c, String sep) {
        Iterator<?> i;
        if (c == null || (!(i = c.iterator()).hasNext())) return "";
        final StringBuilder result = new StringBuilder(String.valueOf(i.next()));
        while (i.hasNext()) result.append(sep).append(i.next());
        return result.toString();
    }

    private static byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private static byte[] httpResponse(byte[] body, int code, String status,
                                       Map<String, String> headers) {
        final byte[] checkedBody = body == null ? EMPTY_BYTE_ARRAY : body;
        final Map<String, String> headersCopy = new LinkedHashMap<String, String>(headers);
        headersCopy.put("Content-Length", String.valueOf(checkedBody.length));
        final StringBuilder head = new StringBuilder("HTTP/1.1 ").append(code)
                .append(' ').append(status).append("\r\n");
        for (final Map.Entry<String, String> header : headersCopy.entrySet())
            head.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        head.append("\r\n");
        return concat(head.toString().getBytes(ASCII), checkedBody);
    }

    private static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (final byte[] array : arrays) length += array.length;
        final byte[] result = new byte[length];
        int pos = 0;
        for (final byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    private static <T> T checkNotNull(T ref, String errorMessage) {
        if (ref == null) throw new NullPointerException(errorMessage);
        return ref;
    }

    private static String checkNotNullOrEmpty(String ref, String errorMessage) {
        checkNotNull(ref, errorMessage);
        if (ref.isEmpty()) throw new IllegalArgumentException(errorMessage);
        return ref;
    }

    private static byte[] checkNotNullOrEmpty(byte[] ref, String errorMessage) {
        checkNotNull(ref, errorMessage);
        if (ref.length == 0) throw new IllegalArgumentException(errorMessage);
        return ref;
    }

    private static int hash(Object... objects) {
        return Arrays.deepHashCode(objects);
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
