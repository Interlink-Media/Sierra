package de.feelix.sierra.manager.storage;

import de.feelix.sierraapi.violation.PunishType;
import lombok.Data;
import de.feelix.sierraapi.history.History;

/**
 * The HistoryDocument class represents a document that contains information about a user's history.
 */
@Data
public class HistoryDocument implements History {

    private final String     username;
    private final String     description;
    private final PunishType punishType;
    private final long       timestamp = System.currentTimeMillis();

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
}
