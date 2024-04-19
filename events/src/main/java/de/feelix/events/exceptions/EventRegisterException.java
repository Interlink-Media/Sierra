package de.feelix.events.exceptions;

/**
 * Exception that is thrown when there is an error with event registration.
 */
public class EventRegisterException extends RuntimeException {

    /**
     * Constructs a new EventRegisterException with the specified detail message.
     *
     * @param message the detail message
     */
    public EventRegisterException(String message) {
        super(message);
    }
}
