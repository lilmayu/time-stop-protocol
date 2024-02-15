package dev.mayuna.timestop.networking.base.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.minlog.Log;
import dev.mayuna.timestop.networking.timestop.Packets;
import dev.mayuna.timestop.networking.timestop.TimeStopMessage;
import dev.mayuna.timestop.networking.timestop.TimeStopPacket;
import dev.mayuna.timestop.networking.timestop.TimeStopPacketSegment;

import java.util.UUID;

/**
 * Registers all classes that are used in the TimeStop protocol
 */
public class TimeStopSerialization {

    private TimeStopSerialization() {
    }

    /**
     * Registers all classes that are used in the TimeStop protocol
     *
     * @param kryo The Kryo instance to register the classes to
     */
    public static void register(Kryo kryo) {
        Log.debug("Registering network classes...");
        long start = System.currentTimeMillis();

        registerJavaClasses(kryo);
        registerTimeStopClasses(kryo);

        Log.debug("Registered network classes in " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Registers all needed Java classes
     *
     * @param kryo The Kryo instance to register the classes to
     */
    private static void registerJavaClasses(Kryo kryo) {
        // Basic
        kryo.register(byte[].class);
        kryo.register(boolean.class);

        // Serializers
        kryo.register(UUID.class, new UUIDSerializer());
    }

    /**
     * Registers all needed TimeStop classes
     *
     * @param kryo The Kryo instance to register the classes to
     */
    private static void registerTimeStopClasses(Kryo kryo) {
        kryo.register(TimeStopPacket.class);
        kryo.register(TimeStopPacketSegment.class);

        // Packets
        kryo.register(TimeStopMessage.class);
        kryo.register(Packets.BasePacket.class);
        kryo.register(Packets.ProtocolVersionExchange.class);
        kryo.register(Packets.AsymmetricKeyExchange.class);
        kryo.register(Packets.SymmetricKeyExchange.class);
        kryo.register(Packets.EncryptedCommunicationRequest.class);
    }
}
