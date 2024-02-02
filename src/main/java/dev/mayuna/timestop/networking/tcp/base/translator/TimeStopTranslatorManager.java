package dev.mayuna.timestop.networking.tcp.base.translator;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages {@link TimeStopTranslator}s
 */
@Getter
public class TimeStopTranslatorManager {

    private final List<TimeStopTranslator> translators = Collections.synchronizedList(new LinkedList<>());

    /**
     * Creates a new translator manager
     */
    public TimeStopTranslatorManager() {
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
            }
        }

        return object;
    }
}
