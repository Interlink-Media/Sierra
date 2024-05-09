package de.feelix.sierraapi;

import de.feelix.sierraapi.events.EventBus;
import de.feelix.sierraapi.module.ModuleGateway;
import de.feelix.sierraapi.server.Server;
import de.feelix.sierraapi.user.UserRepository;

/**
 * The SierraApi interface represents an API for accessing the functionality provided by the Sierra plugin.
 */
public interface SierraApi {

    /**
     * UserRepository is an interface that provides methods for querying user information.
     */
    UserRepository userRepository();

    /**
     * The moduleGateway method returns an instance of ModuleGateway.
     *
     * @return an instance of ModuleGateway
     */
    ModuleGateway moduleGateway();

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
    Server server();
}
