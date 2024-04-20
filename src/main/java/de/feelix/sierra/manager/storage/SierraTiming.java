package de.feelix.sierra.manager.storage;

import de.feelix.sierraapi.timing.Timing;

/**
 * The SierraTiming class is an implementation of the Timing interface.
 * It provides methods to measure and track the timing of an event.
 */
public class SierraTiming implements Timing {

    /**
     * The delay variable represents the delay in milliseconds between the start and end of a timed event.
     * The delay is calculated by subtracting the start time from the end time.
     * A value of -1 indicates that the delay has not been calculated yet.
     */
    private double delay = -1;

    /**
     * The startTime variable represents the starting time of an event.
     * It is used to measure and track the timing of the event.
     * The value is in milliseconds since January 1, 1970, 00:00:00.000 GMT (Unix timestamp).
     * A value of -1 indicates that the start time has not been set yet.
     */
    private long startTime = -1;

    /**
     * Prepares the Timing object for measuring the timing of an event.
     * This method should be called before starting the event that needs to be timed.
     */
    @Override
    public void prepare() {
        this.startTime = System.nanoTime();
    }

    /**
     * Marks the end of the timing measurement and calculates the duration of the event.
     * This method should be called after finishing the event that needs to be timed.
     * It subtracts the start time from the current time to calculate the delay in milliseconds.
     *
     * @throws IllegalStateException if called before calling the {@code prepare()} method
     */
    @Override
    public void end() {
        this.delay = System.nanoTime() - startTime;
    }

    /**
     * Returns the delay in milliseconds between the start and end of the timed event.
     * The delay is calculated by subtracting the start time from the end time.
     *
     * @return the delay in milliseconds
     * @throws IllegalStateException if called before calling the {@code prepare()} method or after calling the
     *                               {@code end()} method
     */
    @Override
    public double delay() {
        return (this.delay / 1000000);
    }
}
