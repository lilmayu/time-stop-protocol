package dev.mayuna.timestop.networking.tcp.base.listener;

import com.esotericsoftware.kryonet.Connection;
import dev.mayuna.timestop.networking.tcp.base.ConnectionContext;
import lombok.Getter;
import lombok.NonNull;

/**
 * Listener for received messages
 *
 * @param <T> Type of message to listen for
 */
@Getter
public abstract class TimeStopListener<T> {

    private final Class<T> listeningClass;
    private final int priority;

    /**
     * Creates a new listener
     *
     * @param listeningClass Class to listen for
     * @param priority       Priority of the listener (higher priority will be executed first)
     */
    public TimeStopListener(Class<T> listeningClass, int priority) {
        this.listeningClass = listeningClass;
        this.priority = priority;
    }

    /**
     * Processes a received message
     *
     * @param context Context for the message
     * @param message Message to process
     */
    public abstract void process(@NonNull TimeStopListener.Context context, @NonNull T message);

    /**
     * Context for received messages. Used for ignoring the message, etc.
     */
    @Getter
    public static class Context extends ConnectionContext {

        private boolean shouldIgnore = false;

        public Context(@NonNull Connection connection) {
            super(connection);
        }

        /**
         * Sets whether the message should be ignored
         *
         * @param shouldIgnore Whether the message should be ignored
         */
        public void setShouldIgnore(boolean shouldIgnore) {
            this.shouldIgnore = shouldIgnore;
        }
    }
}
