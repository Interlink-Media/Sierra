package de.feelix.events.exceptions;

/**
 * Exception thrown when an event cannot be dispatched.
 */
public class EventDispatchException extends RuntimeException {
    
    /**
     * Represents an exception that is thrown when an event cannot be dispatched.
     */
    public EventDispatchException(final String message) {
        super(message);
    }
}
