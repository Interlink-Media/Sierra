package de.feelix.sierraapi.events;

import de.feelix.sierraapi.events.api.SierraAbstractEvent;
import de.feelix.sierraapi.events.priority.ListenerPriority;

import java.util.List;
import java.util.function.Consumer;

/**
 * Represents an event bus that allows events to be published and subscribed to.
 */
public interface EventBus {

    /**
     * Subscribes to an event of a specific type.
     *
     * @param <T>       the type of the event
     * @param eventType the class object representing the type of the event
     * @param event     the consumer that will handle the event
     *
     * @throws IllegalArgumentException if eventType or event is null
     */
    <T extends SierraAbstractEvent> void subscribe(Class<T> eventType, Consumer<T> event);

    /**
     * Subscribes a consumer to handle events of a specific type with a given priority.
     *
     * @param <T>       the type of event to subscribe to, must extend {@link SierraAbstractEvent}
     * @param eventType the class object representing the type of event to subscribe to
     * @param event     the consumer function to handle the event
     * @param priority  the priority of the listener
     *
     * @throws IllegalArgumentException if eventType, event, or priority is null
     */
    <T extends SierraAbstractEvent> void subscribe(Class<T> eventType, Consumer<T> event, ListenerPriority priority);

    /**
     * Publishes an event to the event bus.
     *
     * @param <T>    the type of the event to be published, must extend {@link SierraAbstractEvent}
     * @param event  the event to be published
     *
     * @throws IllegalArgumentException if the event is null
     */
    <T extends SierraAbstractEvent> void publish(T event);

    /**
     * Dispatches an event to all the subscribers that are subscribed to the event type.
     *
     * @param <T>              the type of the event, must extend {@link SierraAbstractEvent}
     * @param event            the event to be dispatched
     * @param eventSubscribers the list of event subscribers to send the event to
     */
    <T extends SierraAbstractEvent> void dispatchEventToSubscribers(T event,
                                                                    List<EventSubscriber<?>> eventSubscribers);
}
