package org.webbitserver.mongrel2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class Mongrel2Request {
    private static final byte SPACE = (byte) ' ';
    private static final byte COLON = (byte) ':';
    private static final String ASCII = "US-ASCII";

    private final JSONObject headers;

    public String header(String name) throws JSONException {
        return headers.getString(name);
    }

    private enum STATE {
        UUID,
        ID,
        PATH,
        HEADERS
    }

    public final String uuid;
    public final String id;
    public final String path;

    public Mongrel2Request(String uuid, String id, String path, String headers) throws JSONException {
        this.uuid = uuid;
        this.id = id;
        this.path = path;
        this.headers = new JSONObject(headers);
    }

    public static Mongrel2Request parse(byte[] bytes) throws UnsupportedEncodingException, JSONException {
        STATE state = STATE.UUID;
        int start = -1;
        String uuid = null;
        String id = null;
        String path = null;
        for (int i = 0; i < bytes.length; i++) {
            if (state == STATE.UUID) {
                if (bytes[i] == SPACE) {
                    uuid = new String(bytes, 0, i);
                    state = STATE.ID;
                    start = i + 1;
                }
            } else if (state == STATE.ID) {
                if (bytes[i] == SPACE) {
                    id = new String(bytes, start, i - start, ASCII);
                    state = STATE.PATH;
                    start = i + 1;
                }
            } else if (state == STATE.PATH) {
                if (bytes[i] == SPACE) {
                    path = new String(bytes, start, i - start, ASCII);
                    state = STATE.HEADERS;
                    start = i + 1;
                }
            } else if (state == STATE.HEADERS) {
                if (bytes[i] == COLON) {
                    String len = new String(bytes, start, i - start, ASCII);
                    int l = Integer.parseInt(len);
                    start = i+1;
                    String headers = new String(bytes, start, l, ASCII);
                    return new Mongrel2Request(uuid, id, path, headers);
                }
            }
        }
        return null;
    }
}
