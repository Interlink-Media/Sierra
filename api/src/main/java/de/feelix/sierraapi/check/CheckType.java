package de.feelix.sierraapi.check;

import lombok.Getter;

/**
 * Enumeration representing different types of checks.
 */
@Getter
public enum CheckType {

    SPAM(1, "Packet Spam"),
    SIGN(2, "Sign Crasher"),
    INVALID(3, "Invalid Packet"),
    BOOK(4, "Book Crasher"),
    COMMAND(5, "Blocked Command"),
    CREATIVE(6, "Creative Crasher"),
    MOVE(7, "Move Crasher");

    /**
     * Represents the unique identifier for a check type.
     */
    private final int    id;
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
