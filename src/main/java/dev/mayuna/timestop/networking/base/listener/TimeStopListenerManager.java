package dev.mayuna.timestop.networking.base.listener;

import com.esotericsoftware.kryonet.Connection;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;

/**
 * Async listener manager for TimeStop
 */
@Getter
public class TimeStopListenerManager {

    private final ThreadPoolExecutor executor;
    private final List<TimeStopListener<?>> listeners = Collections.synchronizedList(new LinkedList<>());

    /**
     * Creates a new listener manager
     *
     * @param maxThreads The maximum amount of threads to use
     */
    public TimeStopListenerManager(int maxThreads) {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
    }

    /**
     * Registers a listener
     *
     * @param listener Listener to register
     */
    public void registerListener(TimeStopListener<?> listener) {
        listeners.add(listener);
    }

    /**
     * Unregisters a listener
     *
     * @param listener Listener to unregister
     */
    public void unregisterListener(TimeStopListener<?> listener) {
        listeners.remove(listener);
    }

    /**
     * Registers a listener that will be unregistered after the first time it was called
     *
     * @param listener Listener to register
     * @param <T>      Type of the message
     */
    public <T> void registerOneTimeListener(TimeStopListener<T> listener) {
        listeners.add(new TimeStopListener<T>(listener.getListeningClass(), listener.getPriority()) {
            @Override
            public void process(@NonNull Context context, @NonNull T message) {
                unregisterListener(this);
                listener.process(context, message);
            }
        });
    }

    /**
     * Registers a listener that will be unregistered and executed if unregisterCondition returns true
     *
     * @param listener            Listener to register
     * @param unregisterCondition Condition to unregister the listener
     * @param <T>                 Type of the message
     */
    public <T> void registerOneTimeListener(TimeStopListener<T> listener, BiFunction<TimeStopListener.Context, T, Boolean> unregisterCondition) {
        listeners.add(new TimeStopListener<T>(listener.getListeningClass(), listener.getPriority()) {
            @Override
            public void process(@NonNull Context context, @NonNull T message) {
                if (!unregisterCondition.apply(context, message)) {
                    return;
                }

                unregisterListener(this);
                listener.process(context, message);
            }
        });
    }

    /**
     * Processes a received message
     *
     * @param connection Connection the message was received from
     * @param object     Message to process
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void process(Connection connection, Object object) {
        executor.execute(() -> {
            TimeStopListener.Context context = new TimeStopListener.Context(connection);

            listeners.stream()
                     .filter(listener -> listener.getListeningClass().isAssignableFrom(object.getClass()))
                     .sorted((listener1, listener2) -> Integer.compare(listener2.getPriority(), listener1.getPriority()))
                     .forEach(listenerWithParameter -> {
                         if (context.isShouldIgnore()) {
                             return;
                         }

                         // Cast to type with parameter
                         ((TimeStopListener) listenerWithParameter).process(context, listenerWithParameter.getListeningClass().cast(object));
                     });
        });
    }
}
