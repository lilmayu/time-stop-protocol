package dev.mayuna.timestop.networking.timestop.translators;

import dev.mayuna.timestop.exceptions.FailedToDecryptPacketException;
import dev.mayuna.timestop.managers.EncryptionManager;
import dev.mayuna.timestop.networking.base.translator.TimeStopTranslator;
import dev.mayuna.timestop.networking.timestop.TimeStopPacket;

import java.util.function.Function;

public class TimeStopPacketEncryptionTranslator {

    /**
     * Creates a new TimeStopPacketEncryptionTranslator with priority 100
     */
    private TimeStopPacketEncryptionTranslator() {
    }

    /**
     * Decrypts a packet
     *
     * @param encryptionManager Encryption manager
     * @param context           Context
     * @param packet            Packet
     *
     * @return Decrypted packet
     */
    private static TimeStopPacket decryptPacket(EncryptionManager encryptionManager, TimeStopTranslator.Context context, TimeStopPacket packet) {
        try {
            byte[] decryptedData = encryptionManager.decryptUsingSymmetricKey(packet.getData());
            packet.setData(decryptedData);
            packet.setEncrypted(false);
        } catch (Exception exception) {
            throw new FailedToDecryptPacketException(context.getConnection());
        }

        return packet;
    }

    /**
     * Encrypts a packet
     *
     * @param encryptionManager Encryption manager
     * @param context           Context
     * @param packet            Packet
     *
     * @return Encrypted packet
     */
    private static TimeStopPacket encryptPacket(EncryptionManager encryptionManager, TimeStopTranslator.Context context, TimeStopPacket packet) {
        try {
            byte[] encryptedData = encryptionManager.encryptUsingSymmetricKey(packet.getData());
            packet.setData(encryptedData);
            packet.setEncrypted(true);
        } catch (Exception exception) {
            throw new FailedToDecryptPacketException(context.getConnection());
        }

        return packet;
    }

    /**
     * Encrypts TimeStopPackets
     */
    public static class Encrypt extends TimeStopTranslator {

        private final EncryptionManager encryptionManager;
        private final Function<Context, Boolean> shouldEncryptConsumer;

        public Encrypt(EncryptionManager encryptionManager, Function<Context, Boolean> shouldEncryptConsumer) {
            super(90);
            this.encryptionManager = encryptionManager;
            this.shouldEncryptConsumer = shouldEncryptConsumer;
        }

        @Override
        public Object translate(Context context, Object object) {
            if (!(object instanceof TimeStopPacket)) {
                return object;
            }

            TimeStopPacket packet = (TimeStopPacket) object;

            if (!shouldEncryptConsumer.apply(context)) {
                return object;
            }

            if (!packet.isEncrypted() && context.getWay() == Context.Way.OUTBOUND) {
                return encryptPacket(encryptionManager, context, packet);
            }

            return object;
        }
    }

    /**
     * Decrypts TimeStopPackets
     */
    public static class Decrypt extends TimeStopTranslator {

        private final EncryptionManager encryptionManager;
        private final Function<Context, Boolean> shouldDecryptConsumer;

        public Decrypt(EncryptionManager encryptionManager, Function<Context, Boolean> shouldDecryptConsumer) {
            super(110);
            this.encryptionManager = encryptionManager;
            this.shouldDecryptConsumer = shouldDecryptConsumer;
        }

        @Override
        public Object translate(Context context, Object object) {
            if (!(object instanceof TimeStopPacket)) {
                return object;
            }

            TimeStopPacket packet = (TimeStopPacket) object;

            if (!shouldDecryptConsumer.apply(context)) {
                return object;
            }

            if (packet.isEncrypted() && context.getWay() == Context.Way.INBOUND) {
                return decryptPacket(encryptionManager, context, packet);
            }

            return object;
        }
    }
}
