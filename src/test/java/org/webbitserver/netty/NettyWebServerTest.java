package org.webbitserver.netty;

import org.junit.Before;
import org.webbitserver.base.AbstractWebServerTest;

public class NettyWebServerTest extends AbstractWebServerTest {
    @Before
    public void createServer() {
        server = new NettyWebServer(9876);
    }
}
