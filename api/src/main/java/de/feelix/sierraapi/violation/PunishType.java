package de.feelix.sierraapi.violation;

/**
 * The PunishType enum represents the different types of punishments that can be applied.
 */
public enum PunishType {

    MITIGATE("mitigated for", "UNAVAILABLE"),
    KICK("kicked for", "Security Kick"),
    BAN("punished for", "Security Ban");

    /**
     * The friendlyMessage variable represents a user-friendly message associated with a punishment type.
     * <p>
     * It is a string that provides a user-friendly message that describes the action being taken as part of a
     * punishment.
     * The message can be used to display information to the user about the punishment that has been applied, such as
     * mitigated, kicked, or punished for a specific reason.
     * <p>
     * This variable is used within the PunishType enum to associate a friendly message with each punishment type.
     * Each punishment type has its own unique friendly message that can be accessed by calling the friendlyMessage()
     * method.
     */
    private final String friendlyMessage;

    /**
     * Represents a history message associated with a punishment type.
     * <p>
     * The historyMessage variable is a string that represents a message associated with a punishment type in the
     * PunishType enum.
     * It provides additional information about the punishment, such as the reason for the punishment or any other
     * relevant details.
     * <p>
     * This variable is used within the PunishType enum to associate a history message with each punishment type.
     * Each punishment type has its own unique history message that can be accessed by calling the historyMessage()
     * method.
     */
    private final String historyMessage;

    /**
     * The PunishType enum represents the different types of punishments that can be applied.
     */
    PunishType(String friendlyMessage, String historyMessage) {
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
     * Returns the history message associated with the punishment type.
     * <p>
     * This method returns the history message that is associated with a punishment type in the PunishType enum. The
     * history message provides additional information about the punishment
     * that has been applied, such as the reason for the punishment or any other relevant details.
     *
     * @return The history message associated with the punishment type.
     */
    public String historyMessage() {
        return historyMessage;
    }
}
