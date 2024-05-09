package de.feelix.sierraapi.server;

import java.util.UUID;

/**
 * The Server interface represents a server and defines its properties and behaviors.
 */
@SuppressWarnings("unused")
public interface Server {

    /**
     * Returns the UUID of the server.
     *
     * @return the UUID of the server
     */
    UUID serverUUID();

    /**
     * Returns the current TPS (Ticks Per Second) of the server.
     *
     * @return the TPS of the server as a double value
     */
    double tps();
}
