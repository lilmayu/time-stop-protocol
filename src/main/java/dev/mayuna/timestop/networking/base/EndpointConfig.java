package dev.mayuna.timestop.networking.base;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for endpoints (client and server)
 */
@Getter @Setter
public class EndpointConfig {

    protected int maxThreads = 1;
    protected boolean closeConnectionsOnTranslationException = true;

}
