package org.webbitserver.mongrel2;

import org.json.JSONException;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

// See request_payloads.txt in mongrel sources
public class Mongrel2RequestTest {
    @Test
    public void parses_simple_request() throws UnsupportedEncodingException, JSONException {
        String raw = "FAKESENDER 0 / 99:{\"PATH\":\"/\",\"accept-encoding\":\"foo\",\"host\":\"default\",\"METHOD\":\"GET\",\"VERSION\":\"HTTP/1.1\",\"URI\":\"/\"},0:,";
        Mongrel2Request req = Mongrel2Request.parse(raw.getBytes("US-ASCII"));
        assertEquals("FAKESENDER", req.uuid);
        assertEquals("0", req.id);
        assertEquals("/", req.path);
        assertEquals("foo", req.header("accept-encoding"));
    }

}
