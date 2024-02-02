package dev.mayuna.timestop.networking.tcp.timestop;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class TimeStopPacket {

    private UUID uuid;
    private byte[] data;
    private boolean requireResponse;
    private UUID responseToUuid;
    private boolean encrypted;

    public TimeStopPacket() {
        uuid = UUID.randomUUID();
    }

    public TimeStopPacket(byte[] data) {
        this();
        this.data = data;
    }
}
