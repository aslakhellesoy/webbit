package org.webbitserver.mongrel2.contrib;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class Request {
    private static final byte SPACE = (byte) ' ';
    private static final byte COLON = (byte) ':';
    private static final String ASCII = "US-ASCII";
    
    private static ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference HEADER_TYPE = new TypeReference<Map<String,String>>(){};
    private Map<String,String> headers;

    public String header(String name)  {
        return headers.get(name);
    }

    public URI uri() {
        return URI.create(headers.get("URI"));
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

    public Request(String uuid, String id, String path, String jsonHeaders) throws IOException {
        this.uuid = uuid;
        this.id = id;
        this.path = path;
        this.headers = mapper.readValue(jsonHeaders, HEADER_TYPE);
    }

    public static Request parse(byte[] bytes) throws IOException {
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
                    return new Request(uuid, id, path, headers);
                }
            }
        }
        return null;
    }
}
