package de.feelix.sierra.manager.storage;

import de.feelix.sierraapi.violation.PunishType;
import lombok.Data;
import de.feelix.sierraapi.history.History;

@Data
public class HistoryDocument implements History {

    private final String     username;
    private final String     description;
    private final PunishType punishType;
    private final long       timestamp = System.currentTimeMillis();

    @Override
    public String username() {
        return username;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public PunishType punishType() {
        return punishType;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }
}
