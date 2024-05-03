package de.feelix.sierraapi.events.impl;

import de.feelix.sierraapi.events.api.SierraAbstractEvent;
import lombok.Getter;

/**
 * AsyncSierraCommandEvent is an event class that is fired asynchronously when a Sierra command is executed.
 * It extends the Bukkit Event class.
 */
@Getter
public class AsyncSierraCommandEventSierra extends SierraAbstractEvent {

    /**
     * The label of a command.
     * <p>
     * This variable represents the label of a command in a Sierra command event. It is a private final field,
     * meaning once assigned a value, it cannot be changed. This ensures the immutability and consistency of
     * the label throughout the lifecycle of the event.
     */
    private final String command;

    /**
     * The label of an AsyncSierraCommandEvent.
     * <p>
     * This variable represents the label of a Sierra command in an
     * AsyncSierraCommandEvent event. It is a private final field, meaning
     * once assigned a value, it cannot be changed. This ensures the
     * immutability and consistency of the label throughout the lifecycle
     * of the event.
     *
     * @see AsyncSierraCommandEventSierra
     */
    private final String label;

    /**
     * Constructor for the AsyncSierraCommandEvent class.
     *
     * @param command the command being executed
     * @param label the alias of the command used
     */
    public AsyncSierraCommandEventSierra(String command, String label) {
        this.command = command;
        this.label = label;
    }
}
