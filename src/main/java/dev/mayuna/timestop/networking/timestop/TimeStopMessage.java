package dev.mayuna.timestop.networking.timestop;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public abstract class TimeStopMessage {

    private UUID timeStopMessageId = UUID.randomUUID();
    private UUID timeStopResponseToMessageId;

    /**
     * Sets the message id
     *
     * @param message The message
     *
     * @return Itself
     */
    public TimeStopMessage withResponseTo(TimeStopMessage message) {
        this.timeStopResponseToMessageId = message.getTimeStopMessageId();
        return this;
    }

    /**
     * Sets the message id
     *
     * @param messageId The message id
     *
     * @return Itself
     */
    public TimeStopMessage withResponseTo(UUID messageId) {
        this.timeStopResponseToMessageId = messageId;
        return this;
    }
}
