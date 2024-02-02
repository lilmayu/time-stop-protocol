package dev.mayuna.timestop;

import dev.mayuna.timestop.config.EncryptionConfig;
import dev.mayuna.timestop.managers.EncryptionManager;
import dev.mayuna.timestop.networking.NetworkConstants;
import dev.mayuna.timestop.networking.tcp.base.EndpointConfig;
import dev.mayuna.timestop.networking.tcp.base.TimeStopClient;
import dev.mayuna.timestop.networking.tcp.base.TimeStopServer;
import dev.mayuna.timestop.networking.tcp.base.listener.TimeStopListener;
import dev.mayuna.timestop.networking.tcp.base.translator.TimeStopTranslator;
import dev.mayuna.timestop.networking.tcp.timestop.translators.TimeStopPacketEncryptionTranslator;
import dev.mayuna.timestop.networking.tcp.timestop.translators.TimeStopPacketSegmentTranslator;
import dev.mayuna.timestop.networking.tcp.timestop.translators.TimeStopPacketTranslator;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TestServerClientCommunication {

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
    public void testUUIDSend() {
        UUID uuidToSend = UUID.randomUUID();
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getListenerManager().registerListener(new TimeStopListener<UUID>(UUID.class, 100) {

            @Override
            public void process(@NonNull Context context, @NonNull UUID message) {
                assertEquals(uuidToSend, message);

                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        client.sendTCP(uuidToSend);

        synchronized (received) {
            assertDoesNotThrow(() -> received.wait(1000));
            assertEquals(true, received.get());
        }
    }

    @Test
    public void testTranslatorUUIDtoUUID() {
        UUID uuidToSend = UUID.randomUUID();
        UUID translatedUUID = UUID.randomUUID();
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getListenerManager().registerListener(new TimeStopListener<UUID>(UUID.class, 0) {

            @Override
            public void process(@NonNull Context context, @NonNull UUID message) {
                assertEquals(translatedUUID, message);

                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        server.getTranslatorManager().registerTranslator(new TimeStopTranslator(0) {
            @Override
            public Object translate(Context context, Object object) {
                if (object instanceof UUID) {
                    return translatedUUID;
                }

                return object;
            }
        });

        client.sendTCP(uuidToSend);

        synchronized (received) {
            assertDoesNotThrow(() -> received.wait(1000));
            assertEquals(true, received.get());
        }
    }

    @Test
    public void testTranslatorUUIDtoNull() {
        UUID uuidToSend = UUID.randomUUID();
        AtomicReference<Boolean> received = new AtomicReference<>(false);
        AtomicReference<Boolean> translatedToNull = new AtomicReference<>(false);

        server.getListenerManager().registerListener(new TimeStopListener<UUID>(UUID.class, 0) {

            @Override
            public void process(@NonNull Context context, @NonNull UUID message) {
                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        server.getTranslatorManager().registerTranslator(new TimeStopTranslator(0) {
            @Override
            public Object translate(Context context, Object object) {
                if (object instanceof UUID) {
                    translatedToNull.set(true);
                    return null;
                }

                return object;
            }
        });

        client.sendTCP(uuidToSend);

        synchronized (translatedToNull) {
            assertDoesNotThrow(() -> {
                synchronized (received) {
                    received.wait(1000);
                }
            });

            assertEquals(false, received.get());
            assertEquals(true, translatedToNull.get());
        }
    }

    @Test
    public void testTranslatorUUIDtoUUIDWithPriority() {
        UUID uuidToSend = UUID.randomUUID();
        UUID translatedUuid = UUID.randomUUID();
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getListenerManager().registerListener(new TimeStopListener<UUID>(UUID.class, 0) {

            @Override
            public void process(@NonNull Context context, @NonNull UUID message) {
                assertEquals(translatedUuid, message);

                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        server.getTranslatorManager().registerTranslator(new TimeStopTranslator(100) {
            @Override
            public Object translate(Context context, Object object) {
                return object;
            }
        });

        server.getTranslatorManager().registerTranslator(new TimeStopTranslator(0) {
            @Override
            public Object translate(Context context, Object object) {
                if (object instanceof UUID) {
                    return translatedUuid;
                }

                return object;
            }
        });

        client.sendTCP(uuidToSend);

        synchronized (received) {
            assertDoesNotThrow(() -> {
                synchronized (received) {
                    received.wait(1000);
                }
            });

            assertEquals(true, received.get());
        }
    }


    @Test
    public void testTranslatorUUIDtoUUIDWithIncorrectPriority() {
        UUID uuidToSend = UUID.randomUUID();
        UUID translatedUuid = UUID.randomUUID();
        AtomicReference<Boolean> received = new AtomicReference<>(false);
        AtomicReference<Boolean> incorrectTranslatorExecuted = new AtomicReference<>(false);

        server.getListenerManager().registerListener(new TimeStopListener<UUID>(UUID.class, 0) {
            @Override
            public void process(@NonNull Context context, @NonNull UUID message) {
                assertNotEquals(translatedUuid, message);

                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        server.getTranslatorManager().registerTranslator(new TimeStopTranslator(100) {
            @Override
            public Object translate(Context context, Object object) {
                assertTrue(incorrectTranslatorExecuted.get());

                return UUID.randomUUID();
            }
        });

        server.getTranslatorManager().registerTranslator(new TimeStopTranslator(101) {
            @Override
            public Object translate(Context context, Object object) {
                incorrectTranslatorExecuted.set(true);

                if (object instanceof UUID) {
                    return translatedUuid;
                }

                return object;
            }
        });

        client.sendTCP(uuidToSend);

        synchronized (received) {
            assertDoesNotThrow(() -> {
                synchronized (received) {
                    received.wait(1000);
                }
            });

            assertTrue(incorrectTranslatorExecuted.get());
            assertEquals(true, received.get());
        }
    }

    @Test
    public void testTimeStopPacketTranslations() {
        UUID uuidToSend = UUID.randomUUID();
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());
        client.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());

        server.getListenerManager().registerListener(new TimeStopListener<UUID>(UUID.class, 0) {
            @Override
            public void process(@NonNull Context context, @NonNull UUID message) {
                assertEquals(uuidToSend, message);

                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        client.sendTCP(uuidToSend);

        synchronized (received) {
            assertDoesNotThrow(() -> {
                synchronized (received) {
                    received.wait(1000);
                }
            });

            assertEquals(true, received.get());
        }
    }

    @Test
    public void testTimeStopPacketTranslationsOtherWayAround() {
        UUID uuidToSend = UUID.randomUUID();
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());
        client.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());

        client.getListenerManager().registerListener(new TimeStopListener<UUID>(UUID.class, 0) {
            @Override
            public void process(@NonNull Context context, @NonNull UUID message) {
                assertEquals(uuidToSend, message);

                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        server.sendToTCP(client.getID(), uuidToSend);

        synchronized (received) {
            assertDoesNotThrow(() -> {
                synchronized (received) {
                    received.wait(1000);
                }
            });

            assertEquals(true, received.get());
        }
    }

    @Test
    public void testTimeStopPacketSegmentTranslations() {
        byte[] bytesToSend = new byte[NetworkConstants.OBJECT_BUFFER_SIZE*2];
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());
        client.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());

        server.getTranslatorManager().registerTranslator(new TimeStopPacketSegmentTranslator(NetworkConstants.OBJECT_BUFFER_SIZE));
        client.getTranslatorManager().registerTranslator(new TimeStopPacketSegmentTranslator(NetworkConstants.OBJECT_BUFFER_SIZE));

        server.getListenerManager().registerListener(new TimeStopListener<byte[]>(byte[].class, 0) {
            @Override
            public void process(@NonNull Context context, @NonNull byte[] message) {
                assertArrayEquals(bytesToSend, message);

                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        client.sendTCP(bytesToSend);

        synchronized (received) {
            assertDoesNotThrow(() -> {
                synchronized (received) {
                    received.wait(1000);
                }
            });

            assertEquals(true, received.get());
        }
    }

    @Test
    public void testTimeStopPacketEncryptionTranslation() throws NoSuchAlgorithmException {
        byte[] bytesToSend = new byte[1000];
        AtomicReference<Boolean> received = new AtomicReference<>(false);

        server.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());
        client.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());

        server.getTranslatorManager().registerTranslator(new TimeStopPacketSegmentTranslator(NetworkConstants.OBJECT_BUFFER_SIZE));
        client.getTranslatorManager().registerTranslator(new TimeStopPacketSegmentTranslator(NetworkConstants.OBJECT_BUFFER_SIZE));

        EncryptionManager encryptionManager = new EncryptionManager(new EncryptionConfig());
        encryptionManager.generateSymmetricKey();

        server.getTranslatorManager().registerTranslator(new TimeStopPacketEncryptionTranslator.Decrypt(encryptionManager, context -> true));
        server.getTranslatorManager().registerTranslator(new TimeStopPacketEncryptionTranslator.Encrypt(encryptionManager, context -> true));

        client.getTranslatorManager().registerTranslator(new TimeStopPacketEncryptionTranslator.Decrypt(encryptionManager, context -> true));
        client.getTranslatorManager().registerTranslator(new TimeStopPacketEncryptionTranslator.Encrypt(encryptionManager, context -> true));

        server.getListenerManager().registerListener(new TimeStopListener<byte[]>(byte[].class, 0) {
            @Override
            public void process(@NonNull Context context, @NonNull byte[] message) {
                assertArrayEquals(bytesToSend, message);

                synchronized (received) {
                    received.set(true);
                    received.notifyAll();
                }
            }
        });

        client.sendTCP(bytesToSend);

        synchronized (received) {
            assertDoesNotThrow(() -> {
                synchronized (received) {
                    received.wait(1000);
                }
            });

            assertEquals(true, received.get());
        }
    }
}
