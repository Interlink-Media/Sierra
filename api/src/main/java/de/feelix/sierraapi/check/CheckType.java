package de.feelix.sierraapi.check;

import lombok.Getter;

@Getter
public enum CheckType {

    SPAM(1, "Packet Spam"),
    SIGN(2, "Sign Crasher"),
    INVALID(3, "Invalid Packet"),
    BOOK(4, "Book Crasher"),
    COMMAND(5, "Blocked Command"),
    CREATIVE(6, "Creative Crasher"),
    MOVE(7, "Move Crasher");

    private final int    id;
    private final String friendlyName;

    CheckType(int id, String friendlyName) {
        this.id = id;
        this.friendlyName = friendlyName;
    }
}
