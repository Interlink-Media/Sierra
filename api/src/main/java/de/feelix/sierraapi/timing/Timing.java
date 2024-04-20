package de.feelix.sierraapi.timing;

/**
 * The Timing interface represents an object that can measure and track the timing of an event.
 * Implementations of this interface can be used to measure the time taken for specific tasks or operations.
 */
public interface Timing {

    /**
     * Prepares the Timing object for measuring the timing of an event.
     * This method should be called before starting the event that needs to be timed.
     */
    void prepare();

    /**
     * Completes the measuring of the timing of an event.
     * This method should be called after finishing the event that needs to be timed.
     * It marks the end of the timing measurement and calculates the duration of the event.
     *
     * @throws IllegalStateException if called before calling the {@code prepare()} method
     */
    void end();

    /**
     * Returns the delay measured by the Timing object.
     *
     * @return the delay measured by the Timing object as a double
     */
    double delay();
}
