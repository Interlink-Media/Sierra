package de.feelix.sierraapi.history;

import de.feelix.sierraapi.violation.PunishType;

/**
 * The History interface represents the history of a user's actions or punishments. It includes information such as
 * the username, description, punishment type, and timestamp.
 */
@SuppressWarnings("unused")
public interface History {

    /**
     * Returns the username associated with the History instance.
     *
     * @return The username as a String.
     */
    String username();

    /**
     * Returns the description associated with the History instance.
     *
     * @return The description as a String.
     */
    String description();

    /**
     * Returns the PunishType of the History instance.
     *
     * @return The PunishType object representing the type of punishment applied.
     */
    PunishType punishType();

    /**
     * Returns the timestamp associated with the instance of History.
     *
     * @return The timestamp as a long value indicating the time in milliseconds since the epoch (January 1, 1970,
     * 00:00:00 GMT).
     */
    long timestamp();

    /**
     * Returns the client version associated with the client.
     *
     * @return The client version as a String.
     */
    String clientVersion();

    /**
     * Returns the HistoryType of the History instance.
     *
     * @return The HistoryType object representing the type of history.
     */
    HistoryType historyType();

    /**
     * Returns the timestamp at which the ping request was sent.
     *
     * @return The timestamp as a long value indicating the time in milliseconds since the epoch (January 1, 1970,
     * 00:00:00 GMT).
     */
    long ping();
}
