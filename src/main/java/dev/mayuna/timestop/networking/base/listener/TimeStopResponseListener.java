package dev.mayuna.timestop.networking.base.listener;

import dev.mayuna.timestop.networking.timestop.TimeStopMessage;
import lombok.Getter;

import java.util.UUID;

/**
 * Listener for received messages
 *
 * @param <T> Type of message to listen for
 */
@Getter
public abstract class TimeStopResponseListener<T> extends TimeStopListener<T> {

    private final UUID responseToMessageId;

    /**
     * Creates a new listener
     *
     * @param listeningClass Class to listen for
     * @param priority       Priority of the listener (higher priority will be executed first)
     */
    public TimeStopResponseListener(Class<T> listeningClass, int priority, UUID responseToMessageId) {
        super(listeningClass, priority);
        this.responseToMessageId = responseToMessageId;
    }

    /**
     * Casts the object to TimeStopMessage if possible and returns the message id
     * @param object Object to cast
     * @return Message id if available (null if not)
     */
    public static UUID getMessageIdIfAvailable(Object object) {
        if (object instanceof TimeStopMessage) {
            return ((TimeStopMessage) object).getTimeStopMessageId();
        }

        return null;
    }
}
