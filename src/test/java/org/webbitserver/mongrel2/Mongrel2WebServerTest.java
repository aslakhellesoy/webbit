package org.webbitserver.mongrel2;

import org.junit.Before;
import org.webbitserver.base.AbstractWebServerTest;

public class Mongrel2WebServerTest extends AbstractWebServerTest {
    @Before
    public void createServer() {
        server = new Mongrel2WebServer();
    }
}
