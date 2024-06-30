package de.feelix.sierra.manager.storage.history;

import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.history.HistoryType;
import de.feelix.sierraapi.violation.MitigationStrategy;
import lombok.AllArgsConstructor;
import de.feelix.sierraapi.history.History;
import lombok.Data;

@Data
@AllArgsConstructor
public class HistoryDocument implements History {

    private final String username;
    private final String description;
    private final String clientVersion;

    private final long ping;

    private final MitigationStrategy mitigationStrategy;
    private final HistoryType        historyType;

    private final long timestamp = System.currentTimeMillis();

    @Override
    public String username() {
        return username;
    }


    @Override
    public String description() {
        return description;
    }


    @Override
    public MitigationStrategy mitigationStrategy() {
        return mitigationStrategy;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String clientVersion() {
        return clientVersion;
    }

    @Override
    public HistoryType historyType() {
        return historyType;
    }

    @Override
    public long ping() {
        return ping;
    }

    public String formatTimestamp() {
        return FormatUtils.formatTimestamp(this.timestamp);
    }

    public String shortenDescription() {
        return FormatUtils.shortenString(this.description);
    }
}
