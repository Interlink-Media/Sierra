package de.feelix.sierra.manager.event;

import de.feelix.sierraapi.events.api.SierraAbstractEvent;
import de.feelix.sierraapi.events.EventSubscriber;
import de.feelix.sierraapi.events.priority.ListenerPriority;
import de.feelix.sierraapi.events.EventBus;

import java.util.*;
import java.util.function.Consumer;

/**
 * Represents an event bus that allows events to be published and subscribed to.
 */
public class AbstractEventBus implements EventBus {

    /**
     * Represents a map that holds a list of event subscribers for each event type.
     */
    private final Map<Class<?>, List<EventSubscriber<?>>> subscribers = new HashMap<>();

    /**
     * Subscribes a consumer to handle events of a specific type.
     *
     * @param <T>       the type of the event
     * @param eventType the class object representing the event type
     * @param event     the consumer to handle the event
     */
    public <T extends SierraAbstractEvent> void subscribe(Class<T> eventType, Consumer<T> event) {
        this.subscribe(eventType, event, ListenerPriority.NORMAL);
    }

    /**
     * Subscribes a consumer to handle events of a specific type with a given priority.
     *
     * @param <T>       the type of event to subscribe to, must extend {@link SierraAbstractEvent}
     * @param eventType the class object representing the type of event to subscribe to
     * @param event     the consumer function to handle the event
     * @param priority  the priority of the listener
     */
    public <T extends SierraAbstractEvent> void subscribe(Class<T> eventType, Consumer<T> event, ListenerPriority priority) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>())
            .add(new EventSubscriber<>(event, priority.getScore()));
    }

    /**
     * Publishes an event to all the subscribers that are subscribed to the event type.
     * If no subscribers are found for the given event type, the method returns without any further action.
     *
     * @param <T>   the type of event to be published, must extend AbstractEvent
     * @param event the event to be published
     */
    public <T extends SierraAbstractEvent> void publish(T event) {
        List<EventSubscriber<?>> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers == null) return;

        dispatchEventToSubscribers(event, eventSubscribers);
    }

    /**
     * Dispatches an event to all the subscribers that are subscribed to the event type.
     *
     * @param <T>              the type of the event, must extend {@link SierraAbstractEvent}
     * @param event            the event to be dispatched
     * @param eventSubscribers the list of event subscribers to send the event to
     */
    public <T extends SierraAbstractEvent> void dispatchEventToSubscribers(T event,
                                                                           List<EventSubscriber<?>> eventSubscribers) {

        eventSubscribers.sort(Comparator.comparingInt(EventSubscriber::getPriority));

        for (EventSubscriber<?> eventSubscriber : eventSubscribers) {
            ((EventSubscriber<T>) eventSubscriber).getConsumer().accept(event);
        }
    }
}
