package de.feelix.sierraapi.violation;

import de.feelix.sierraapi.annotation.NotNull;

public enum MitigationStrategy {

    MITIGATE("mitigated for", "UNAVAILABLE"),
    KICK("kicked for", "Security Kick"),
    BAN("punished for", "Security Ban");

    @NotNull
    private final String friendlyMessage;


    @NotNull
    private final String historyMessage;

    MitigationStrategy(String friendlyMessage, String historyMessage) {
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


    public String historyMessage() {
        return historyMessage;
    }
}
