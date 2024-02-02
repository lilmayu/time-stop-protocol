package dev.mayuna.timestop.networking.base.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.UUID;

/**
 * Serializer for UUIDs
 */
public class UUIDSerializer extends Serializer<UUID> {

    @Override
    public void write(Kryo kryo, Output output, UUID object) {
        output.writeLong(object.getMostSignificantBits());
        output.writeLong(object.getLeastSignificantBits());
    }

    @Override
    public UUID read(Kryo kryo, Input input, Class<? extends UUID> type) {
        return new UUID(input.readLong(), input.readLong());
    }
}
