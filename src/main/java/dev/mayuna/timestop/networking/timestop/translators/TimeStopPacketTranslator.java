package dev.mayuna.timestop.networking.timestop.translators;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryonet.FrameworkMessage;
import dev.mayuna.timestop.networking.base.translator.TimeStopTranslator;
import dev.mayuna.timestop.networking.timestop.TimeStopPacket;

import java.nio.ByteBuffer;

public class TimeStopPacketTranslator extends TimeStopTranslator {

    public static int BUFFER_SIZE = 64_000_000; // 64MB

    /**
     * Creates a new TimeStopPacketTranslator with priority 100
     */
    public TimeStopPacketTranslator() {
        super(100);
    }

    @Override
    public Object translate(Context context, Object object) {
        if (object instanceof FrameworkMessage) {
            return object;
        }

        // TimeStopPacket => Object
        if (object instanceof TimeStopPacket && context.getWay() == Context.Way.INBOUND) {
            TimeStopPacket timeStopPacket = (TimeStopPacket) object;

            Kryo kryo = context.getConnection().getEndPoint().getKryo();
            ByteBuffer buffer = ByteBuffer.wrap(timeStopPacket.getData());
            return kryo.readClassAndObject(new ByteBufferInput(buffer));
        }

        // Object => TimeStopPacket
        if (!(object instanceof TimeStopPacket) && context.getWay() == Context.Way.OUTBOUND) {
            Kryo kryo = context.getConnection().getEndPoint().getKryo();

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            kryo.writeClassAndObject(new ByteBufferOutput(buffer), object);

            ByteBuffer compactedBuffer = ByteBuffer.allocate(buffer.position());
            buffer.flip();
            compactedBuffer.put(buffer);
            compactedBuffer.flip();
            return new TimeStopPacket(compactedBuffer.array());
        }

        return object;
    }
}
