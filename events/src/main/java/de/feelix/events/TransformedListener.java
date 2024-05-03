package de.feelix.events;

import de.feelix.events.listeners.ListenerPriority;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * Represents a listener that has been transformed and registered in the EventManager.
 */
@Data
@AllArgsConstructor
public class TransformedListener {

    /**
     * Represents an instance of a listener class.
     */
    private final Object           listenerClassInstance;

    /**
     * Represents a method that is invoked by a listener.
     */
    private final Method           listenerMethod;

    /**
     * Represents the priority of an event listener.
     *
     * <p>
     * The priority determines the order in which the listeners are invoked when an event occurs.
     * The priorities are defined in the {@link ListenerPriority} enum with the following values*/
    private final ListenerPriority priority;
}
