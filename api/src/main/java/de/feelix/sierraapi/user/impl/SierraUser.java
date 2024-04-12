package de.feelix.sierraapi.user.impl;

import de.feelix.sierraapi.check.CheckRepository;

import java.util.UUID;

/**
 * The SierraUser interface represents a user in the Sierra API.
 */
public interface SierraUser {

    /**
     * Retrieves the username of the user.
     *
     * @return The username of the user.
     */
    String username();

    /**
     * Gets the entity ID of the SierraUser.
     *
     * @return The entity ID of the SierraUser.
     */
    int entityId();

    /**
     * Generates a universally unique identifier (UUID) for the SierraUser.
     *
     * @return A UUID representing the SierraUser.
     */
    UUID uuid();

    /**
     * Retrieves the timestamp when the instance of SierraUser was created.
     *
     * @return The timestamp when the SierraUser instance was created.
     */
    long existSince();

    /**
     * Retrieves the version of the SierraUser.
     *
     * @return The version of the SierraUser as a String.
     */
    String version();

    /**
     * The kick method is used to kick the user associated with the PlayerData object.
     * If the user is not null, it will close the user's connection and return true.
     * If the user is null, it will return false.
     *
     * @return true if the user is successfully kicked, false otherwise
     */
    boolean kick();

    /**
     * Returns whether the player is exempt from certain actions or checks.
     *
     * @return true if the player is exempt, false otherwise
     */
    boolean isExempt();

    /**
     * Sets whether the player is exempt from certain actions or checks.
     *
     * @param b true to exempt the player, false to remove exemption
     * @return true if the exemption setting is successfully changed, false otherwise
     */
    boolean setExempt(boolean b);

    /**
     * Returns whether the user has any alerts.
     *
     * @return true if the user has alerts, false otherwise.
     */
    boolean isAlerts();

    /**
     * Sets whether the user has alerts.
     *
     * @param b true to enable alerts, false to disable alerts
     * @return true if the alerts setting is successfully changed, false otherwise
     */
    boolean setAlerts(boolean b);

    /**
     * The CheckRepository interface represents a repository of checks in the Sierra API.
     */
    CheckRepository checkRepository();
}
