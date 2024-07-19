package de.feelix.sierraapi.violation;

import de.feelix.sierraapi.annotation.NotNull;

public enum MitigationStrategy {

    MITIGATE(1, "mitigated for", "UNAVAILABLE"),
    KICK(2, "kicked for", "Security Kick"),
    BAN(3, "punished for", "Security Ban");

    @NotNull
    private final String friendlyMessage;

    @NotNull
    private final int mitigationOrdinal;

    @NotNull
    private final String historyMessage;

    MitigationStrategy(int mitigationOrdinal, String friendlyMessage, String historyMessage) {
        this.mitigationOrdinal = mitigationOrdinal;
        this.friendlyMessage = friendlyMessage;
        this.historyMessage = historyMessage;
    }

    /**
     * Returns the friendly message associated with the punishment type.
     *
     * @return The friendly message associated with the punishment type.
     */
    public String friendlyMessage() {
        return friendlyMessage;
    }

    /**
     * Retrieves the history message associated with the mitigation strategy.
     *
     * @return The history message associated with the mitigation strategy.
     */
    public String historyMessage() {
        return historyMessage;
    }

    /**
     * Retrieves the mitigation ordinal associated with the mitigation strategy.
     *
     * @return The mitigation ordinal.
     */
    public int mitigationOrdinal() {
        return mitigationOrdinal;
    }
}
