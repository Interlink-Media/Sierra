package de.feelix.sierra.manager.server;

import de.feelix.sierraapi.server.Server;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;

import java.util.UUID;

/**
 * The ServerManager class implements the Server interface and represents a server manager.
 * It provides methods to retrieve the UUID of the server and the TPS (Ticks Per Second).
 */
public class ServerManager implements Server {

    /**
     * The uuid variable represents a UUID (Universally Unique Identifier) that is generated using the randomUUID() method from the UUID class.
     * <p>
     * The purpose of this variable is to provide a unique identifier for the server.
     * <p>
     * Use the serverUUID() method of the ServerManager class to retrieve the UUID value assigned to this variable.
     * <p>
     * Example Usage:
     * <pre>
     *     ServerManager serverManager = new ServerManager();
     *     UUID serverUUID = serverManager.serverUUID();
     * </pre>
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * Retrieves the UUID of the server.
     *
     * @return the UUID of the server
     */
    @Override
    public UUID serverUUID() {
        return uuid;
    }

    /**
     * Returns the current TPS (Ticks Per Second) of the server.
     *
     * @return the TPS of the server as a double value
     */
    @Override
    public double tps() {
        return SpigotReflectionUtil.getTPS();
    }
}
