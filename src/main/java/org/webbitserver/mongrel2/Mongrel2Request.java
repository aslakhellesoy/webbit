package org.webbitserver.mongrel2;

public class Mongrel2Request {
    private static final byte SPACE = (byte) ' ';

    private enum STATE {
        UUID,
        ID,
        PATH
    }

    public final String uuid;
    public final String id;
    public final String path;

    public Mongrel2Request(String uuid, String id, String path) {
        this.uuid = uuid;
        this.id = id;
        this.path = path;
    }

    public static Mongrel2Request parse(byte[] bytes) {
        STATE state = STATE.UUID;
        int start = -1;
        String uuid = null;
        String id = null;
        String path;
        for (int i = 0; i < bytes.length; i++) {
            if (state == STATE.UUID) {
                if (bytes[i] == SPACE) {
                    uuid = new String(bytes, 0, i);
                    state = STATE.ID;
                    start = i + 1;
                }
            } else if (state == STATE.ID) {
                if (bytes[i] == SPACE) {
                    id = new String(bytes, start, i - start);
                    state = STATE.PATH;
                    start = i + 1;
                }
            } else if (state == STATE.PATH) {
                if (bytes[i] == SPACE) {
                    path = new String(bytes, start, i - start);
//                    state = STATE.HEADERS;
//                    start = i + 1;
                    return new Mongrel2Request(uuid, id, path);
                }
            }
        }
        return null;
    }
}
