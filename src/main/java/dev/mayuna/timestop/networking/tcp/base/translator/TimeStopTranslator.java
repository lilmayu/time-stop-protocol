package dev.mayuna.timestop.networking.tcp.base.translator;

import com.esotericsoftware.kryonet.Connection;
import dev.mayuna.timestop.networking.tcp.base.ConnectionContext;
import lombok.Getter;
import lombok.NonNull;

/**
 * Translates objects before they are sent/received from/to other endpoint(s).
 */
@Getter
public abstract class TimeStopTranslator {

    private final int priority;

    /**
     * Creates a new translator with the given priority.
     *
     * @param priority Priority of the translator (higher priority will be executed first)
     */
    public TimeStopTranslator(int priority) {
        this.priority = priority;
    }

    /**
     * Translates the given object.<br>If the object is not supported, it should be returned as-is.<br>If the returned object is null, the object will
     * be ignored and no further translators will be executed (not even listeners).
     *
     * @param context Connection context
     * @param object  Object to translate
     *
     * @return Translated object
     */
    public abstract Object translate(Context context, Object object);

    /**
     * Context for received/sent messages.
     */
    @Getter
    public static class Context extends ConnectionContext {

        private final Way way;
        private boolean reset;

        /**
         * Creates a new context
         *
         * @param connection Connection the message was received from / will be sent to
         * @param way        Way of the message
         */
        public Context(@NonNull Connection connection, @NonNull Way way) {
            super(connection);
            this.way = way;
        }

        /**
         * Sets whether the message should be translated from the start again
         *
         * @param reset Whether the message should be translated from the start again
         */
        @SuppressWarnings({"LombokSetterMayBeUsed", "RedundantSuppression"})
        public void setReset(boolean reset) {
            this.reset = reset;
        }

        /**
         * Gets the way of the message
         */
        public enum Way {
            INBOUND,
            OUTBOUND;
        }
    }
}
