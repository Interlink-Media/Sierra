package de.feelix.sierraapi;

import de.feelix.sierraapi.events.EventBus;
import de.feelix.sierraapi.server.SierraServer;
import de.feelix.sierraapi.user.UserRepository;

/**
 * The SierraApi interface represents an API for accessing the functionality provided by the Sierra plugin.
 */
@SuppressWarnings("unused")
public interface SierraApi {

    /**
     * UserRepository is an interface that provides methods for querying user information.
     */
    UserRepository userRepository();

    /**
     * The eventBus method returns an instance of EventBus.
     *
     * @return an instance of EventBus.
     */
    EventBus eventBus();

    /**
     * Retrieves the Server instance which represents a server.
     *
     * @return the Server instance representing the server
     */
    SierraServer server();
}
