package dev.mayuna.timestop.networking.timestop;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Time stop packets are used when sending data over the Time Stop Protocol-enabled network
 */
@Getter @Setter
public class TimeStopPacket {

    private UUID uuid;
    private byte[] data;
    private boolean encrypted;

    public TimeStopPacket() {
        uuid = UUID.randomUUID();
    }

    public TimeStopPacket(byte[] data) {
        this();
        this.data = data;
    }
}
