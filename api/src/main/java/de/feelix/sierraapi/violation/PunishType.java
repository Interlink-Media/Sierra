package de.feelix.sierraapi.violation;

/**
 * The PunishType enum represents the different types of punishments that can be applied.
 */
public enum PunishType {

    MITIGATE("mitigated for"),
    KICK("kicked for"),
    BAN("punished for");

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
     * The PunishType enum represents the different types of punishments that can be applied.
     */
    PunishType(String friendlyMessage) {
        this.friendlyMessage = friendlyMessage;
    }

    /**
     * Returns the friendly message associated with the punishment type.
     *
     * @return The friendly message associated with the punishment type.
     */
    public String friendlyMessage() {
        return friendlyMessage;
    }
}
