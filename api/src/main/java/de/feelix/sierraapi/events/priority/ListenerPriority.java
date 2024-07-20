package de.feelix.sierraapi.events.priority;

import lombok.Getter;

/**
 * A priority for event priority.
 */
@Getter
public enum ListenerPriority {

    HIGH(2),
    DEFAULT(1),
    LOW(0);

    /**
     * The score for event priority.
     */
    private final int score;

    /**
     * Represents the priority of an event listener.
     * The lower the score, the higher the priority.
     *
     * @param score The score for the event priority.
     */
    ListenerPriority(int score) {
        this.score = score;
    }
}
