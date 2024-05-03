package de.feelix.sierraapi.events.api;

import lombok.Data;

/**
 * Represents an abstract event that can be dispatched and listened to.
 */
@Data
@SuppressWarnings("unused")
public abstract class AbstractEvent {

    /**
     * Represents whether an event has been cancelled.
     * <p>
     * The cancelled variable is initially set to false. It can be set to true if the event is cancelled.
     * The cancelled state can be checked using the {@link #isCancelled()} method.
     * The cancelled state can be modified using the {@link #setCancelled(boolean)} method.
     * -- GETTER --
     * Checks whether the event has been cancelled.
     * <p>
     * This method returns a boolean value indicating whether the event has been cancelled. If the event is cancelled,
     * it means that its execution will be interrupted or its effects will be prevented.
     * <p>
     * <p>
     * -- SETTER --
     * Sets the cancelled state of the event.
     * <p>
     * This method sets the cancelled state of the event to the specified value. If the event is cancelled, it means
     * that
     * its execution will be interrupted or its effects will be prevented. The cancelled state can be checked using the
     * <p>
     * method.
     */
    private boolean cancelled = false;

}
