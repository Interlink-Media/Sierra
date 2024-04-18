package de.feelix.sierra.manager.storage;

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

    private final String      username;
    private final String      description;
    private final long        ping;
    private final PunishType  punishType;
    private final HistoryType historyType;
    private final long        timestamp = System.currentTimeMillis();

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
