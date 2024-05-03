package de.feelix.sierraapi.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Consumer;

/**
 * Represents an event subscriber that can consume events of a specific type with a given priority.
 *
 * @param <T> the type of the event to be consumed
 */
@Getter
@AllArgsConstructor
public class EventSubscriber<T> {

    /**
     * Represents a consumer for a specific type.
     */
    public Consumer<T> consumer;

    /**
     * Represents the priority level of an event subscriber.
     */
    private int priority;
}