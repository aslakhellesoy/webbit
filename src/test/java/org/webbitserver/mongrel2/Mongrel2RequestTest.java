package org.webbitserver.mongrel2;

import org.junit.Test;
import org.webbitserver.mongrel2.contrib.Request;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

// See request_payloads.txt in mongrel sources
public class Mongrel2RequestTest {
    @Test
    public void parses_simple_request() throws IOException {
        String raw = "FAKESENDER 0 / 99:{\"PATH\":\"/\",\"accept-encoding\":\"foo\",\"host\":\"default\",\"METHOD\":\"GET\",\"VERSION\":\"HTTP/1.1\",\"URI\":\"/\"},0:,";
        Request req = Request.parse(raw.getBytes("US-ASCII"));
        assertEquals("FAKESENDER", req.uuid);
        assertEquals("0", req.id);
        assertEquals("/", req.path);
        assertEquals("foo", req.header("accept-encoding"));
    }

}
