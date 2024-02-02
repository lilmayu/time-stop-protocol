package dev.mayuna.timestop.networking.base;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import dev.mayuna.timestop.networking.NetworkConstants;
import dev.mayuna.timestop.networking.base.listener.TimeStopListener;
import dev.mayuna.timestop.networking.base.listener.TimeStopListenerManager;
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
 * TimeStopClient
 */
@Getter
public class TimeStopClient extends Client implements Listener {

    private final Timer timeoutTimer = new Timer();
    private final EndpointConfig endpointConfig;
    private TimeStopListenerManager listenerManager;
    private TimeStopTranslatorManager translatorManager;

    /**
     * Creates a new client with the given endpoint config
     *
     * @param endpointConfig   Endpoint config
     * @param writeBufferSize  Write buffer size
     * @param objectBufferSize Object buffer size
     */
    public TimeStopClient(EndpointConfig endpointConfig, int writeBufferSize, int objectBufferSize) {
        super(writeBufferSize, objectBufferSize);
        this.endpointConfig = endpointConfig;
        prepare();
    }

    /**
     * Creates a new client with the given endpoint config
     *
     * @param endpointConfig Endpoint config
     */
    public TimeStopClient(EndpointConfig endpointConfig) {
        this(endpointConfig, NetworkConstants.WRITE_BUFFER_SIZE, NetworkConstants.OBJECT_BUFFER_SIZE);
    }

    /**
     * Prepares the client for usage
     */
    private void prepare() {
        Log.info("Preparing client...");

        // Listener & translator manager
        listenerManager = new TimeStopListenerManager(endpointConfig.getMaxThreads());
        translatorManager = new TimeStopTranslatorManager(endpointConfig.isCloseConnectionsOnTranslationException());

        // Register classes
        TimeStopSerialization.register(getKryo());

        // Register self listener
        addListener(this);
    }

    /**
     * Sends the given object to the server<br>Object will be translated before sending using {@link TimeStopTranslatorManager}.
     *
     * @param object Object to send
     *
     * @return Number of bytes sent (0 when object was translated to null)
     */
    @Override
    public int sendTCP(Object object) {
        object = translatorManager.process(new TimeStopTranslator.Context(this, TimeStopTranslator.Context.Way.OUTBOUND), object);

        if (object == null) {
            return 0;
        }

        return super.sendTCP(object);
    }

    /**
     * Sends the given object to the server<br>Object will be translated before sending using {@link TimeStopTranslatorManager}.
     *
     * @param object Object to send
     *
     * @return Number of bytes sent (0 when object was translated to null)
     */
    @Override
    public int sendUDP(Object object) {
        object = translatorManager.process(new TimeStopTranslator.Context(this, TimeStopTranslator.Context.Way.OUTBOUND), object);

        if (object == null) {
            return 0;
        }

        return super.sendUDP(object);
    }

    /**
     * Sends the given object to the server and waits for a response<br>Object will be translated before sending using
     * {@link TimeStopTranslatorManager}.
     *
     * @param object        Object to send
     * @param responseClass Class of the response
     * @param onResponse    Consumer that will be called when the response is received
     * @param <T>           Type of the response
     *
     * @return Number of bytes sent (0 when object was translated to null)
     */
    public <T> int sendTCPWithResponse(Object object, Class<T> responseClass, Consumer<T> onResponse) {
        listenerManager.registerOneTimeListener(new TimeStopListener<T>(responseClass, 0) {
            @Override
            public void process(@NonNull Context context, @NonNull T message) {
                onResponse.accept(message);
            }
        });

        return sendTCP(object);
    }

    /**
     * Sends the given object to the server and waits for a response<br>Object will be translated before sending using
     * {@link TimeStopTranslatorManager}.
     *
     * @param object        Object to send
     * @param responseClass Class of the response
     * @param timeout       Timeout in milliseconds
     * @param onResponse    Consumer that will be called when the response is received
     * @param onTimeout     Runnable that will be called when the timeout elapsed
     * @param <T>           Type of the response
     *
     * @return Number of bytes sent (0 when object was translated to null)
     */
    public <T> int sendTCPWithResponse(Object object, Class<T> responseClass, int timeout, Consumer<T> onResponse, Runnable onTimeout) {
        AtomicReference<Runnable> timeoutRunnable = new AtomicReference<>(null);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // Will cancel the timer task and run onTimeout
                timeoutRunnable.get().run();
            }
        };

        // Create listener
        TimeStopListener<T> listener = new TimeStopListener<T>(responseClass, 0) {
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
        listenerManager.registerOneTimeListener(listener);

        return sendTCP(object);
    }

    @Override
    public void received(Connection connection, Object object) {
        object = translatorManager.process(new TimeStopTranslator.Context(connection, TimeStopTranslator.Context.Way.INBOUND), object);

        if (object == null) {
            return;
        }

        listenerManager.process(connection, object);
    }
}
