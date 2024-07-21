package de.feelix.sierraapi.check;

import lombok.Getter;

/**
 * Enumeration representing different types of checks.
 */
@Getter
public enum CheckType {

    FREQUENCY(1, "Frequency"),
    PROTOCOL_VALIDATION(2, "Protocol Validation"),
    BOOK_VALIDATION(3, "Book Validation"),
    COMMAND_VALIDATION(4, "Command Validation"),
    CREATIVE(5, "Creative Crasher"),
    MOVEMENT_VALIDATION(6, "Movement Validation"),
    POST(7, "Post Protocol");

    /**
     * Represents the unique identifier for a check type.
     */
    private final int id;

    /**
     * Represents the friendly name of a check type.
     *
     * <p>
     * The friendly name is a human-readable name that describes a specific type of check.
     * </p>
     *
     * <p>
     * It is used to provide meaningful and descriptive information about the purpose or function of a check type.
     * </p>
     *
     * @since 1.0
     */
    private final String friendlyName;

    /**
     * Represents a check type with a unique identifier and friendly name.
     */
    CheckType(int id, String friendlyName) {
        this.id = id;
        this.friendlyName = friendlyName;
    }
}
