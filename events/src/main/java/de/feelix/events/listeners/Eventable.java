package de.feelix.events.listeners;

import java.lang.annotation.*;

/**
 * Annotation used to mark methods as event handlers.
 * Event handlers are methods that will be called when a specific event is dispatched.
 * The priority of event handlers can be specified using the {@link Eventable#priority()} attribute.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Eventable {

    /**
     * Returns the priority of the listener method.
     *
     * @return the priority of the listener method
     */
    ListenerPriority priority() default ListenerPriority.NORMAL;
}
