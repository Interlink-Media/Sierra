package de.feelix.sierraapi.events.priority;

import lombok.Getter;

/**
 * A priority for event priority.
 */
@Getter
public enum ListenerPriority {

    HIGHEST(5),
    HIGH(4),
    NORMAL(3),
    LOW(2),
    LOWEST(1);

    private final int score;

    ListenerPriority(int score) {
        this.score = score;
    }
}
