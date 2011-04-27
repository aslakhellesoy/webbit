package org.webbitserver.base;

import org.junit.After;
import org.junit.Test;
import org.webbitserver.*;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.webbitserver.testutil.HttpClient.contents;
import static org.webbitserver.testutil.HttpClient.httpGet;

public abstract class AbstractWebServerTest {
    protected WebServer server;
    private static final Charset ASCII = Charset.forName("US-ASCII");

    @After
    public void stopServer() throws IOException, InterruptedException {
        server.stop().join();
    }

    @Test
    public void servesSimplheRequests() throws IOException {
        server.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest req, HttpResponse res, HttpControl ctl) throws Exception {
                res.content("Hello World".getBytes(ASCII)).end();
            }
        }).start();

        long start = System.currentTimeMillis();
        int reqs = 10000;
        for (int i = 0; i < reqs; i++) {
            String result = contents(httpGet(server, "/"));
            assertEquals("Hello World", result);
        }
        long millis = System.currentTimeMillis() - start;
        double secs = millis/1000.0;
        double reqsPerSec = reqs/secs;
        System.out.println("Regs/sec:" + reqsPerSec);
    }

}
