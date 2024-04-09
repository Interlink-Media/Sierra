package de.feelix.sierraapi.violation;

public enum PunishType {

    MITIGATE("mitigated for"),
    KICK("kicked for"),
    BAN("punished for");


    private final String friendlyMessage;

    PunishType(String friendlyMessage) {
        this.friendlyMessage = friendlyMessage;
    }

    public String friendlyMessage() {
        return friendlyMessage;
    }
}
