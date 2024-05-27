package de.feelix.sierra.manager.storage;

import de.feelix.sierra.Sierra;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * The AddressStorage class is responsible for storing and managing IP addresses
 * along with their corresponding added time.
 */
public class AddressStorage {

    /**
     * The ipAddresses variable is a private final Map object that stores the IP addresses along with their
     * corresponding added time.
     * The Map is implemented using the HashMap class.
     * The keys in the Map represent the IP addresses, while the values represent the corresponding added time in
     * LocalDateTime format.
     * The variable is declared with the "private" access modifier, which means it can only be accessed within the
     * class it is defined in.
     * It is also declared with the "final" modifier, which means its value cannot be changed after initialization.
     * <p>
     * Example usage:
     * <p>
     * private final Map<String, LocalDateTime> ipAddresses = new HashMap<>();
     */
    private final Map<String, LocalDateTime> ipAddresses = new HashMap<>();

    /**
     * Adds the given IP address to the AddressStorage object.
     * The IP address is associated with the current LocalDateTime.
     *
     * @param ipAddress the IP address to be added
     */
    public void addIPAddress(String ipAddress) {
        if (ipAddress.equalsIgnoreCase("127.0.0.1")) return;
        ipAddresses.put(ipAddress, LocalDateTime.now());
    }

    /**
     * Checks if the given IP address is considered invalid based on its added time.
     *
     * @param ipAddress the IP address to check
     * @return true if the IP address is invalid, false otherwise
     */
    public boolean invalid(String ipAddress) {
        LocalDateTime addedTime = ipAddresses.get(ipAddress);
        if (addedTime == null) {
            return false;
        }

        if (isOlderThanFifteenMinutes(addedTime)) {
            removeIPAddress(ipAddress);
            return false;
        }
        return true;
    }

    /**
     * Checks if the given addedTime is older than fifteen minutes.
     *
     * @param addedTime the LocalDateTime representing the time the IP address was added
     * @return true if the addedTime is older than fifteen minutes, false otherwise
     */
    private boolean isOlderThanFifteenMinutes(LocalDateTime addedTime) {
        Duration duration = Duration.between(addedTime, LocalDateTime.now());
        return duration.toMinutes() > Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getInt("connection-block-time", 15);
    }

    /**
     * Removes the given IP address from the AddressStorage object.
     * The IP address will no longer be associated with its corresponding added time.
     *
     * @param ipAddress the IP address to be removed
     */
    private void removeIPAddress(String ipAddress) {
        ipAddresses.remove(ipAddress);
    }
}
