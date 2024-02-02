package dev.mayuna.timestop.networking.base;

import com.esotericsoftware.kryonet.Connection;
import lombok.Getter;
import lombok.NonNull;

/**
 * Context for received/sent messages.
 */
@Getter
public class ConnectionContext {

    private final Connection connection;

    /**
     * Creates a new context
     *
     * @param connection Connection the message was received from / will be sent to
     */
    public ConnectionContext(@NonNull Connection connection) {
        this.connection = connection;
    }
}