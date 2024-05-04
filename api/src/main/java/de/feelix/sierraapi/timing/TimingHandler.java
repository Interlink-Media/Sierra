package de.feelix.sierraapi.timing;

/**
 * The TimingHandler interface represents an object that provides timing tasks for various operations.
 * The implementation of this interface should provide methods to retrieve different Timing objects
 * for measuring the timing of specific tasks or operations.
 */
public interface TimingHandler {

    /**
     * Retrieves the movement task Timing object.
     *
     * @return the Timing object for measuring the timing of the movement task
     */
    Timing getMovementTask();

    /**
     * Retrieves the Timing object for measuring the timing of the packet receive task.
     *
     * @return the Timing object for measuring the timing of the packet receive task
     *
     * @see TimingHandler#getPacketReceiveTask()
     * @see Timing
     */
    Timing getPacketReceiveTask();

    /**
     * Retrieves the Timing object for measuring the timing of the packet send task.
     *
     * @return the Timing object for measuring the timing of the packet send task
     *
     * @see TimingHandler#getPacketSendTask()
     * @see Timing
     */
    Timing getPacketSendTask();
}
