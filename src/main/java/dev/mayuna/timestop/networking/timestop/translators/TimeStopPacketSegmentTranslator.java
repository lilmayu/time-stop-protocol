package dev.mayuna.timestop.networking.timestop.translators;

import com.esotericsoftware.kryonet.FrameworkMessage;
import dev.mayuna.timestop.networking.base.translator.TimeStopTranslator;
import dev.mayuna.timestop.networking.timestop.TimeStopPacket;
import dev.mayuna.timestop.networking.timestop.TimeStopPacketSegment;

import java.util.*;

public class TimeStopPacketSegmentTranslator extends TimeStopTranslator {

    private final List<TimeStopPacketSegment> segments = Collections.synchronizedList(new LinkedList<>());

    private final int maxSegmentSize;

    /**
     * Creates a new LargeTimeStopPacketTranslator with priority 50
     *
     * @param maxSegmentSize Maximum size of a single segment
     */
    public TimeStopPacketSegmentTranslator(int maxSegmentSize) {
        super(50);
        this.maxSegmentSize = maxSegmentSize;
    }

    @Override
    public Object translate(Context context, Object object) {
        if (object instanceof FrameworkMessage) {
            return object;
        }

        switch (context.getWay()) {
            case OUTBOUND: {
                if (object instanceof TimeStopPacket) {
                    return createSegments(context, (TimeStopPacket) object);
                }

                break;
            }
            case INBOUND: {
                if (object instanceof TimeStopPacketSegment) {
                    context.setReset(true);
                    return receiveSegment((TimeStopPacketSegment) object);
                }

                break;
            }
        }

        return object;
    }

    private TimeStopPacket createSegments(Context context, TimeStopPacket timeStopPacket) {
        if (timeStopPacket.getData().length <= maxSegmentSize) {
            // No need to split
            return timeStopPacket;
        }

        int maxSegmentSizeWithSpace = maxSegmentSize - 256;

        int segmentCount = (int) Math.ceil((double) timeStopPacket.getData().length / maxSegmentSizeWithSpace);

        for (int i = 0; i < segmentCount; i++) {
            int offset = i * maxSegmentSizeWithSpace;
            int length = Math.min(maxSegmentSizeWithSpace, timeStopPacket.getData().length - offset);

            byte[] segmentData = new byte[length];
            System.arraycopy(timeStopPacket.getData(), offset, segmentData, 0, length);

            context.getConnection().sendTCP(new TimeStopPacketSegment(timeStopPacket.getUuid(), segmentData, i, segmentCount));
        }

        return null;
    }

    private TimeStopPacket receiveSegment(TimeStopPacketSegment timeStopPacketSegment) {
        segments.add(timeStopPacketSegment);
        List<TimeStopPacketSegment> segments = getSegmentsByParentUuidIfComplete(timeStopPacketSegment.getParentUuid(), timeStopPacketSegment.getSegmentCount());

        // Non complete packet
        if (segments == null) {
            return null;
        }

        // Complete packet
        segments.sort(Comparator.comparingInt(TimeStopPacketSegment::getSegmentIndex));

        int totalLength = 0;

        for (TimeStopPacketSegment segment : segments) {
            totalLength += segment.getData().length;
        }

        byte[] data = new byte[totalLength];

        int offset = 0;

        for (TimeStopPacketSegment segment : segments) {
            System.arraycopy(segment.getData(), 0, data, offset, segment.getData().length);
            offset += segment.getData().length;
        }

        return new TimeStopPacket(data);
    }

    private List<TimeStopPacketSegment> getSegmentsByParentUuidIfComplete(UUID parentUuid, int segmentCount) {
        List<TimeStopPacketSegment> segments = new ArrayList<>();

        synchronized (segments) {
            for (TimeStopPacketSegment segment : this.segments) {
                if (segment.getParentUuid().equals(parentUuid)) {
                    segments.add(segment);
                }
            }
        }

        if (segments.size() == segmentCount) {
            return segments;
        }

        return null;
    }
}
