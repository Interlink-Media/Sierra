package de.feelix.sierra.manager.storage.history;

import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.history.HistoryType;
import de.feelix.sierraapi.violation.PunishType;
import lombok.AllArgsConstructor;
import de.feelix.sierraapi.history.History;

/**
 * The HistoryDocument class represents a document that contains information about a user's history.
 */
@AllArgsConstructor
public class HistoryDocument implements History {

    /**
     * The username associated with this HistoryDocument.
     */
    private final String username;

    /**
     * The private final variable "description" represents the description associated with a History instance.
     * <p>
     * It is a string that provides additional details or information about the user's action or punishment.
     * This variable is declared as "private final" to ensure that it cannot be modified once it is assigned a value.
     * <p>
     * This variable is part of the class "HistoryDocument" which is an implementation of the "History" interface.
     * The "HistoryDocument" class provides methods to retrieve information such as the username, punishType, timestamp,
     * and historyType associated with the history instance.
     * <p>
     * This variable is also used in methods such as "description()" and "shortenDescription()" to retrieve or modify
     * the
     * description of the history instance.
     */
    private final String description;

    /**
     * The clientVersion represents the version of the client that was used for an action or punishment in the history.
     * It is a String that provides information about the version of the client software used by a user.
     * The clientVersion is a final variable, meaning its value cannot be changed once initialized.
     */
    private final String clientVersion;

    /**
     * Player ping
     */
    private final long ping;

    /**
     * The punishType variable represents the type of punishment associated with a history document.
     * It is an instance of the PunishType enum, which defines different types of punishments that can be applied.
     *
     * @see PunishType
     * @see HistoryDocument
     */
    private final PunishType punishType;

    /**
     * The `HistoryType` enum represents the type of history that can be associated with a user's actions or
     * punishments.
     */
    private final HistoryType historyType;

    /**
     * The timestamp variable represents a long value indicating the time in milliseconds since the epoch (January 1,
     * 1970, 00:00:00 GMT).
     * It is a private and final instance variable of the class HistoryDocument.
     * The timestamp variable is initialized with the current system time using the System.currentTimeMillis() method.
     * <p>
     * Example usage:
     * <p>
     * HistoryDocument document = new HistoryDocument();
     * long timestamp = document.timestamp();
     */
    private final long timestamp = System.currentTimeMillis();

    /**
     * Retrieves the username associated with this HistoryDocument.
     *
     * @return The username as a String.
     */
    @Override
    public String username() {
        return username;
    }

    /**
     * Returns the description associated with this HistoryDocument.
     *
     * @return The description as a String.
     */
    @Override
    public String description() {
        return description;
    }

    /**
     * Retrieves the type of punishment associated with this HistoryDocument.
     *
     * @return The punishType as a PunishType object.
     */
    @Override
    public PunishType punishType() {
        return punishType;
    }

    /**
     * Returns the timestamp associated with this HistoryDocument.
     *
     * @return The timestamp as a long representing the number of milliseconds since January 1, 1970, 00:00:00 GMT.
     */
    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String clientVersion() {
        return clientVersion;
    }

    /**
     * Returns the history type associated with this HistoryDocument.
     *
     * @return The historyType as a HistoryType object representing the type of history.
     */
    @Override
    public HistoryType historyType() {
        return historyType;
    }

    /**
     * Returns the timestamp at which the ping request was sent.
     *
     * @return The timestamp as a long value indicating the time in milliseconds since the epoch (January 1, 1970,
     * 00:00:00 GMT).
     */
    @Override
    public long ping() {
        return ping;
    }

    /**
     * Formats the timestamp associated with this HistoryDocument to a formatted string representation.
     *
     * @return The formatted string representation of the timestamp.
     */
    public String formatTimestamp() {
        return FormatUtils.formatTimestamp(this.timestamp);
    }

    /**
     * Shortens the description string associated with a HistoryDocument instance if its length is greater than 50
     * characters.
     *
     * @return The shortened description string if its length is greater than 50 characters, otherwise the original
     * description string.
     */
    public String shortenDescription() {
        return FormatUtils.shortenString(this.description);
    }
}
