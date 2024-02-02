package dev.mayuna.timestop.networking.tcp.base;

import com.esotericsoftware.kryonet.Connection;
import dev.mayuna.timestop.networking.tcp.base.listener.TimeStopListenerManager;
import dev.mayuna.timestop.networking.tcp.base.translator.TimeStopTranslator;
import dev.mayuna.timestop.networking.tcp.base.translator.TimeStopTranslatorManager;
import lombok.Getter;
import lombok.Setter;

import java.security.Key;

@Getter @Setter
public class TimeStopConnection extends Connection {

    private final TimeStopListenerManager listenerManager;
    private final TimeStopTranslatorManager translatorManager;

    private Key publicKey;
    private boolean encryptDataSentOverNetwork = false;

    /**
     * Creates a new connection with the given translator manager
     *
     * @param listenerManager listener manager
     * @param translatorManager Translator manager
     */
    public TimeStopConnection(TimeStopListenerManager listenerManager, TimeStopTranslatorManager translatorManager) {
        super();

        this.listenerManager = listenerManager;
        this.translatorManager = translatorManager;
    }

    /**
     * Sends the given object to the server<br>Object will be translated before sending using {@link TimeStopTranslatorManager}.
     *
     * @param object Object to send
     *
     * @return Number of bytes sent (0 when object was translated to null)
     */
    @Override
    public int sendTCP(Object object) {
        object = translatorManager.process(new TimeStopTranslator.Context(this, TimeStopTranslator.Context.Way.OUTBOUND), object);

        if (object == null) {
            return 0;
        }

        return super.sendTCP(object);
    }

    /**
     * Sends the given object to the server<br>Object will be translated before sending using {@link TimeStopTranslatorManager}.
     *
     * @param object Object to send
     *
     * @return Number of bytes sent (0 when object was translated to null)
     */
    @Override
    public int sendUDP(Object object) {
        object = translatorManager.process(new TimeStopTranslator.Context(this, TimeStopTranslator.Context.Way.OUTBOUND), object);

        if (object == null) {
            return 0;
        }

        return super.sendUDP(object);
    }
}
