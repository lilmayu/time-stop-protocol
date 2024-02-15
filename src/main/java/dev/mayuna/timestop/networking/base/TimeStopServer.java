package dev.mayuna.timestop.networking.base;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import dev.mayuna.timestop.networking.NetworkConstants;
import dev.mayuna.timestop.networking.base.listener.TimeStopListener;
import dev.mayuna.timestop.networking.base.listener.TimeStopListenerManager;
import dev.mayuna.timestop.networking.base.listener.TimeStopResponseListener;
import dev.mayuna.timestop.networking.base.serialization.TimeStopSerialization;
import dev.mayuna.timestop.networking.base.translator.TimeStopTranslator;
import dev.mayuna.timestop.networking.base.translator.TimeStopTranslatorManager;
import lombok.Getter;
import lombok.NonNull;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * TimeStopServer
 */
@Getter
public class TimeStopServer extends Server implements Listener {

    private final Timer timeoutTimer = new Timer();
    private final EndpointConfig endpointConfig;
    private TimeStopListenerManager listenerManager;
    private TimeStopTranslatorManager translatorManager;

    /**
     * Creates a new server with the given endpoint config
     *
     * @param endpointConfig   Endpoint config
     * @param writeBufferSize  Write buffer size
     * @param objectBufferSize Object buffer size
     */
    public TimeStopServer(EndpointConfig endpointConfig, int writeBufferSize, int objectBufferSize) {
        super(writeBufferSize, objectBufferSize);
        this.endpointConfig = endpointConfig;
        prepare();
    }

    /**
     * Creates a new server with the given endpoint config
     *
     * @param endpointConfig Endpoint config
     */
    public TimeStopServer(EndpointConfig endpointConfig) {
        this(endpointConfig, NetworkConstants.WRITE_BUFFER_SIZE, NetworkConstants.OBJECT_BUFFER_SIZE);
    }

    /**
     * Prepares the server for usage
     */
    private void prepare() {
        Log.info("Preparing server...");

        // Listener & translator manager
        listenerManager = new TimeStopListenerManager(endpointConfig.getMaxThreads());
        translatorManager = new TimeStopTranslatorManager(endpointConfig.isCloseConnectionsOnTranslationException());

        // Register classes
        TimeStopSerialization.register(getKryo());

        // Register self listener
        addListener(this);
    }

    /**
     * Creates a new TimeStopConnection with current listener manager and translator manager
     *
     * @return Connection
     */
    @Override
    protected Connection newConnection() {
        return new TimeStopConnection(listenerManager, translatorManager);
    }

    /**
     * Processes received objects. Translates them using {@link TimeStopTranslatorManager} and then passes them to {@link TimeStopListenerManager}.
     *
     * @param connection Connection
     * @param object     Object
     */
    @Override
    public void received(Connection connection, Object object) {
        object = translatorManager.process(new TimeStopTranslator.Context(connection, TimeStopTranslator.Context.Way.INBOUND), object);

        if (object == null) {
            return;
        }

        listenerManager.process(connection, object);
    }

    /**
     * Sends the given object to the server<br>Object will be translated before sending using {@link TimeStopTranslatorManager}.
     *
     * @param connection Connection
     * @param object     Object to send
     */
    public void sendToTCP(Connection connection, Object object) {
        sendToTCP(connection.getID(), object);
    }

    /**
     * Sends the given object to the server and waits for a response<br>Object will be translated before sending using
     * {@link TimeStopTranslatorManager}.
     *
     * @param connection    Connection
     * @param object        Object to send
     * @param responseClass Class of the response
     * @param onResponse    Consumer that will be called when the response is received
     * @param <T>           Type of the response
     */
    public <T> void sendTCPWithResponse(Connection connection, Object object, Class<T> responseClass, Consumer<T> onResponse) {
        listenerManager.registerOneTimeListener(new TimeStopResponseListener<T>(responseClass, 0, TimeStopResponseListener.getMessageIdIfAvailable(object)) {
            @Override
            public void process(@NonNull Context context, @NonNull T message) {
                onResponse.accept(message);
            }
        });

        sendToTCP(connection, object);
    }

    /**
     * Sends the given object to the server and waits for a response<br>Object will be translated before sending using
     * {@link TimeStopTranslatorManager}.
     *
     * @param connection    Connection
     * @param object        Object to send
     * @param responseClass Class of the response
     * @param timeout       Timeout in milliseconds
     * @param onResponse    Consumer that will be called when the response is received
     * @param onTimeout     Runnable that will be called when the timeout elapsed
     * @param <T>           Type of the response
     */
    public <T> void sendToTCPWithResponse(Connection connection, Object object, Class<T> responseClass, int timeout, Consumer<T> onResponse, Runnable onTimeout) {
        AtomicReference<Runnable> timeoutRunnable = new AtomicReference<>(null);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // Will cancel the timer task and run onTimeout
                timeoutRunnable.get().run();
            }
        };

        // Create listener
        TimeStopResponseListener<T> listener = new TimeStopResponseListener<T>(responseClass, 0, TimeStopResponseListener.getMessageIdIfAvailable(object)) {
            @Override
            public void process(@NonNull Context context, @NonNull T message) {
                timerTask.cancel();
                onResponse.accept(message);
            }
        };

        // Set timeout runnable
        timeoutRunnable.set(() -> {
            listenerManager.unregisterListener(listener);
            onTimeout.run();
        });

        // Schedule timer
        timeoutTimer.schedule(timerTask, timeout);

        // Register one time listener
        listenerManager.registerOneTimeListener(listener, (context, message) -> context.getConnection().getID() == connection.getID());

        sendToTCP(connection, object);
    }
}
