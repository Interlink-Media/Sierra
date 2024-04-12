package de.feelix.sierraapi.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * AsyncSierraCommandEvent is an event class that is fired asynchronously when a Sierra command is executed.
 * It extends the Bukkit Event class.
 */
@Getter
public class AsyncSierraCommandEvent extends Event {

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
     * @see AsyncSierraCommandEvent
     */
    private final String label;

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    /**
     * Constructor for the AsyncSierraCommandEvent class.
     *
     * @param command the command being executed
     * @param label the alias of the command used
     */
    public AsyncSierraCommandEvent(String command, String label) {
        super(true);
        this.command = command;
        this.label = label;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
