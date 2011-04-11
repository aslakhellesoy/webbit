package org.webbitserver.mongrel2;

import com.paperculture.mongrel2.Handler;
import org.json.JSONException;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class Mongrel2WebServerTest {
    @Test
    public void parses_stuff() throws UnsupportedEncodingException, JSONException {
        //          0         1         2         3         4         5
        //          01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
        byte[] b = "049f77fe-646d-11e0-901e-bf9ab42ab7ab 6 /blah 246:{\"PATH\":\"/blah\",\"x-forwarded-for\":\"127.0.0.1\",\"accept\":\"*/*\",\"user-agent\":\"curl/7.19.7 (universal-apple-darwin10.0) libcurl/7.19.7 OpenSSL/0.9.8l zlib/1.2.3\",\"host\":\"localhost:6767\",\"METHOD\":\"GET\",\"VERSION\":\"HTTP/1.1\",\"URI\":\"/blah\",\"PATTERN\":\"/\"},0:,".getBytes("UTF-8");
        //Handler.Request req = Handler.Request.parse(b, false);
        Mongrel2Request req = Mongrel2Request.parse(b);
        System.out.println("req = " + req);
    }

}
