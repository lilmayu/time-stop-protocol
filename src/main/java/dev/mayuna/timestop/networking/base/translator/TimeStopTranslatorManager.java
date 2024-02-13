package dev.mayuna.timestop.networking.base.translator;

import com.esotericsoftware.minlog.Log;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages {@link TimeStopTranslator}s
 */
@Getter
public class TimeStopTranslatorManager {

    protected final List<TimeStopTranslator> translators = Collections.synchronizedList(new LinkedList<>());
    private final boolean closeConnectionsOnTranslationException;

    /**
     * Creates a new translator manager
     * @param closeConnectionsOnTranslationException If true, connections will be closed if an exception occurs while translating
     */
    public TimeStopTranslatorManager(boolean closeConnectionsOnTranslationException) {
        this.closeConnectionsOnTranslationException = closeConnectionsOnTranslationException;
    }

    /**
     * Registers a translator
     *
     * @param translator Translator to register
     */
    public void registerTranslator(TimeStopTranslator translator) {
        translators.add(translator);
        translators.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
    }

    /**
     * Unregisters a translator
     *
     * @param translator Translator to unregister
     */
    public void unregisterTranslator(TimeStopTranslator translator) {
        translators.remove(translator);
    }

    /**
     * Processes message<br>If some translator returns null, returned value will be null (and other translators will not be executed)
     *
     * @param context Translating context
     * @param object  Message to process
     *
     * @return Translated message
     */
    public Object process(TimeStopTranslator.Context context, Object object) {
        synchronized (translators) {
            for (TimeStopTranslator translator : translators) {
                try {
                    // TODO: Catch errors and close connections
                    object = translator.translate(context, object);

                    if (context.isReset()) {
                        context.setReset(false);
                        object = process(context, object);
                    }

                    // If the translated object is null, we should not continue
                    if (object == null) {
                        return null;
                    }
                } catch (Exception exception) {
                    Log.error("Error while translating message for connection '" + context.getConnection().toString()  + "'." + (closeConnectionsOnTranslationException ? " The connection will be terminated." : ""), exception);

                    if (closeConnectionsOnTranslationException) {
                        context.getConnection().close();
                    }

                    return null;
                }
            }
        }

        return object;
    }
}
