package de.feelix.sierraapi.user.impl;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import de.feelix.sierraapi.check.CheckRepository;
import de.feelix.sierraapi.timing.TimingHandler;
import de.feelix.sierraapi.user.settings.AlertSettings;

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
     * Retrieves the brand of the user.
     *
     * @return The brand of the user as a String.
     */
    String brand();

    /**
     * Gets the entity ID of the SierraUser.
     *
     * @return The entity ID of the SierraUser.
     */
    int entityId();

    /**
     * The ping method sends a ping request and returns the round trip time in milliseconds.
     *
     * @return The round trip time in milliseconds as an integer.
     */
    int ping();

    /**
     * Returns the number of ticks that the instance has existed for.
     *
     * @return The number of ticks that the instance has existed for.
     */
    int ticksExisted();

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
     * Retrieves the game mode of the player.
     *
     * @return The game mode of the player as an instance of GameMode.
     */
    GameMode gameMode();

    /**
     * Sets whether the player is exempt from certain actions or checks.
     *
     * @param b true to exempt the player, false to remove exemption
     * @return true if the exemption setting is successfully changed, false otherwise
     */
    boolean setExempt(boolean b);

    /**
     * Retrieves the alert settings for the user.
     *
     * @return The alert settings for the user as an instance of {@link AlertSettings}.
     *
     * @see SierraUser#alertSettings()
     */
    AlertSettings alertSettings();

    /**
     * Retrieves the mitigation settings for the user.
     *
     * @return The mitigation settings for the user as*/
    AlertSettings mitigationSettings();

    /**
     * The CheckRepository interface represents a repository of checks in the Sierra API.
     */
    CheckRepository checkRepository();

    /**
     * The TimingHandler interface represents an object that provides timing tasks for various operations.
     * The implementation of this interface should provide methods to retrieve different Timing objects
     * for measuring the timing of specific tasks or operations.
     *
     * @return The TimingHandler object.
     */
    TimingHandler timingHandler();
}
