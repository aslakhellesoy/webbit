package samples.eventsource;

import org.webbitserver.CometConnection;
import org.webbitserver.CometHandler;
import org.webbitserver.WebServer;
import org.webbitserver.handler.EmbeddedResourceHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.webbitserver.WebServers.createWebServer;

public class Main {
    public static class Pusher implements Runnable {
        private List<CometConnection> connections = new ArrayList<CometConnection>();
        private int count = 1;

        public Pusher(final Executor webThread) throws InterruptedException {
            Executor pusherThread = newSingleThreadExecutor();
            pusherThread.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        webThread.execute(Pusher.this);
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        @Override
        public void run() {
            broadcast(new Date().toString());
        }

        private void broadcast(String message) {
            for (CometConnection connection : connections) {
                connection.send(message);
            }
        }

        public void addConnection(CometConnection connection) {
            connection.data("id", count++);
            connections.add(connection);
            broadcast("Client " + connection.data("id") + " joined");
        }

        public void removeConnection(CometConnection connection) {
            connections.remove(connection);
            broadcast("Client " + connection.data("id") + " left");
        }
    }

    public static void main(String[] args) throws Exception {
        Executor webThread = newSingleThreadExecutor();
        final Pusher pusher = new Pusher(webThread);
        WebServer webServer = createWebServer(webThread, 9876)
                .add("/events", new CometHandler() {
                    @Override
                    public void onOpen(CometConnection connection) throws Exception {
                        pusher.addConnection(connection);
                    }

                    @Override
                    public void onClose(CometConnection connection) throws Exception {
                        pusher.removeConnection(connection);
                    }

                    @Override
                    public void onMessage(CometConnection connection, String msg) throws Exception {
                        throw new IllegalStateException("Should never happen");
                    }
                })
                .add(new EmbeddedResourceHandler("samples/eventsource/content"))
                .start();

        System.out.println("EventSource demo running on: " + webServer.getUri());
    }
}
