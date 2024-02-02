package dev.mayuna.timestop;

import dev.mayuna.timestop.networking.NetworkConstants;
import dev.mayuna.timestop.networking.tcp.base.EndpointConfig;
import dev.mayuna.timestop.networking.tcp.base.TimeStopClient;
import dev.mayuna.timestop.networking.tcp.base.TimeStopServer;
import dev.mayuna.timestop.networking.tcp.base.listener.TimeStopListener;
import dev.mayuna.timestop.networking.tcp.timestop.Packets;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TestTimeStopPackets {

    private static final String HOST = "127.0.0.1";

    private static TimeStopServer server;
    private static TimeStopClient client;

    @BeforeAll
    public static void setup() {
        server = new TimeStopServer(new EndpointConfig());
        assertDoesNotThrow(() -> server.bind(NetworkConstants.DEFAULT_PORT));
        server.start();

        client = new TimeStopClient(new EndpointConfig());
        client.start();
        assertDoesNotThrow(() -> client.connect(5000, HOST, NetworkConstants.DEFAULT_PORT));
    }

    @AfterAll
    public static void teardown() {
        assertDoesNotThrow(() -> client.close());
        assertDoesNotThrow(() -> server.close());
    }

    @SneakyThrows
    @BeforeEach
    public void reset() {
        server.getListenerManager().getListeners().clear();
        client.getListenerManager().getListeners().clear();

        server.getTranslatorManager().getTranslators().clear();
        client.getTranslatorManager().getTranslators().clear();
    }

    @Test
    public void sendClientAndWaitForResponse() {
        Packets.ProtocolVersionExchange packet = new Packets.ProtocolVersionExchange(1);
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getListenerManager().registerListener(new TimeStopListener<Packets.ProtocolVersionExchange>(Packets.ProtocolVersionExchange.class, 0) {
            @Override
            public void process(@NonNull Context context, Packets.@NonNull ProtocolVersionExchange message) {
                context.getConnection().sendTCP(new Packets.ProtocolVersionExchange(1));
            }
        });

        client.sendTCPWithResponse(packet, Packets.ProtocolVersionExchange.class, response -> {
            assertEquals(1, response.getProtocolVersion());

            synchronized (received) {
                received.set(true);
                received.notifyAll();
            }
        });

        synchronized (received) {
            assertDoesNotThrow(() -> received.wait(1000));
            assertEquals(true, received.get());
        }
    }

    @Test
    public void sendServerAndWaitForResponse() {
        Packets.ProtocolVersionExchange packet = new Packets.ProtocolVersionExchange(1);
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        client.getListenerManager().registerListener(new TimeStopListener<Packets.ProtocolVersionExchange>(Packets.ProtocolVersionExchange.class, 0) {
            @Override
            public void process(@NonNull Context context, Packets.@NonNull ProtocolVersionExchange message) {
                context.getConnection().sendTCP(new Packets.ProtocolVersionExchange(1));
            }
        });

        server.sendTCPWithResponse(client, packet, Packets.ProtocolVersionExchange.class, response -> {
            assertEquals(1, response.getProtocolVersion());

            synchronized (received) {
                received.set(true);
                received.notifyAll();
            }
        });

        synchronized (received) {
            assertDoesNotThrow(() -> received.wait(1000));
            assertEquals(true, received.get());
        }
    }

    @Test
    public void sendClientAndWaitForResponseWithTimeout() {
        Packets.ProtocolVersionExchange packet = new Packets.ProtocolVersionExchange(1);
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getListenerManager().registerListener(new TimeStopListener<Packets.ProtocolVersionExchange>(Packets.ProtocolVersionExchange.class, 0) {
            @SneakyThrows
            @Override
            public void process(@NonNull Context context, Packets.@NonNull ProtocolVersionExchange message) {
                Thread.sleep(100); // Wait 100ms to trigger timeout
                context.getConnection().sendTCP(new Packets.ProtocolVersionExchange(1));
            }
        });

        client.sendTCPWithResponse(packet, Packets.ProtocolVersionExchange.class, 50, response -> {
            fail("Client received response even after the timeout has ran out"); // Should not be called
        }, () -> {
            synchronized (received) {
                received.set(true);
                received.notifyAll();
            }
        });

        synchronized (received) {
            assertDoesNotThrow(() -> received.wait(1000));
            assertEquals(true, received.get());
        }
    }

    @Test
    public void sendServerAndWaitForResponseWithTimeout() {
        Packets.ProtocolVersionExchange packet = new Packets.ProtocolVersionExchange(1);
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        client.getListenerManager().registerListener(new TimeStopListener<Packets.ProtocolVersionExchange>(Packets.ProtocolVersionExchange.class, 0) {
            @SneakyThrows
            @Override
            public void process(@NonNull Context context, Packets.@NonNull ProtocolVersionExchange message) {
                Thread.sleep(100); // Wait 100ms to trigger timeout
                context.getConnection().sendTCP(new Packets.ProtocolVersionExchange(1));
            }
        });

        server.sendToTCPWithResponse(client, packet, Packets.ProtocolVersionExchange.class, 50, response -> {
            fail("Server received response even after the timeout has ran out"); // Should not be called
        }, () -> {
            synchronized (received) {
                received.set(true);
                received.notifyAll();
            }
        });

        synchronized (received) {
            assertDoesNotThrow(() -> received.wait(1000));
            assertEquals(true, received.get());
        }
    }

    @Test
    public void sendClientAndWaitForResponseWithTimeoutNotElapsed() {
        Packets.ProtocolVersionExchange packet = new Packets.ProtocolVersionExchange(1);
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getListenerManager().registerListener(new TimeStopListener<Packets.ProtocolVersionExchange>(Packets.ProtocolVersionExchange.class, 0) {
            @SneakyThrows
            @Override
            public void process(@NonNull Context context, Packets.@NonNull ProtocolVersionExchange message) {
                Thread.sleep(100); // Wait 100ms
                context.getConnection().sendTCP(new Packets.ProtocolVersionExchange(1));
            }
        });

        client.sendTCPWithResponse(packet, Packets.ProtocolVersionExchange.class, 200, response -> {
            synchronized (received) {
                received.set(true);
                received.notifyAll();
            }
        }, () -> {
            fail("Client timeout elapsed"); // Should not be called
        });

        synchronized (received) {
            assertDoesNotThrow(() -> received.wait(1000));
            assertEquals(true, received.get());
        }
    }

    @Test
    public void sendServerAndWaitForResponseWithTimeoutNotElapsed() {
        Packets.ProtocolVersionExchange packet = new Packets.ProtocolVersionExchange(1);
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        client.getListenerManager().registerListener(new TimeStopListener<Packets.ProtocolVersionExchange>(Packets.ProtocolVersionExchange.class, 0) {
            @SneakyThrows
            @Override
            public void process(@NonNull Context context, Packets.@NonNull ProtocolVersionExchange message) {
                Thread.sleep(100); // Wait 100ms to trigger timeout
                context.getConnection().sendTCP(new Packets.ProtocolVersionExchange(1));
            }
        });

        server.sendToTCPWithResponse(client, packet, Packets.ProtocolVersionExchange.class, 200, response -> {
            synchronized (received) {
                received.set(true);
                received.notifyAll();
            }
        }, () -> {
            fail("Server timeout elapsed"); // Should not be called
        });

        synchronized (received) {
            assertDoesNotThrow(() -> received.wait(1000));
            assertEquals(true, received.get());
        }
    }
}
