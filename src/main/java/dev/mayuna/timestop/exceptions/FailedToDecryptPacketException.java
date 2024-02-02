package dev.mayuna.timestop.exceptions;

import com.esotericsoftware.kryonet.Connection;
import dev.mayuna.timestop.Generated;

@Generated
public class FailedToDecryptPacketException extends RuntimeException {

    public FailedToDecryptPacketException() {
        super();
    }

    public FailedToDecryptPacketException(Connection connection) {
        super("Failed to decrypt packet from connection " + connection.toString());
    }
}
