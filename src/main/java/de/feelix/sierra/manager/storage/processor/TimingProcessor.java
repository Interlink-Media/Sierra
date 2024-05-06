package de.feelix.sierra.manager.storage.processor;

import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.timings.SierraTiming;
import de.feelix.sierraapi.timing.Timing;
import de.feelix.sierraapi.timing.TimingHandler;
import lombok.Getter;

/**
 * The TimingProcessor class is responsible for processing timing information related to player actions.
 */
@Getter
public class TimingProcessor implements TimingHandler {

    /**
     * The playerData variable represents the player data associated with a player.
     * It is of type PlayerData, a class that contains various data and functionality related to players.
     * This variable is declared as private and final, indicating that it cannot be reassigned once initialized.
     * It is also marked with the @Getter annotation, indicating that a getter method has been generated for
     * accessing its value.
     */
    private final PlayerData playerData;

    /**
     * The packetReceiveTiming variable represents the timing object used to measure and track the delay of receiving
     * packets.
     * It is of type Timing, which is an interface for objects that can measure and track the timing of events.
     * The packetReceiveTiming variable is declared as private and final, indicating that it cannot be reassigned
     * once initialized.
     * The object is instantiated as a SierraTiming, which is an implementation of the Timing interface.
     */
    private final Timing packetReceiveTiming = new SierraTiming();

    /**
     * packetSendTiming is a timing object used to measure and track the delay of sending packets.
     * It is of type Timing, which is an interface for objects that can measure and track the timing of events.
     * The packetSendTiming variable is declared as private and final, indicating that it cannot be reassigned once
     * initialized.
     * The object is instantiated as a SierraTiming, which is an implementation of the Timing interface.
     */
    private final Timing packetSendTiming = new SierraTiming();

    /**
     * The movementProcessor variable represents the timing object used to measure and track the delay of movement processing.
     * It is of type Timing, which is an interface for objects that can measure and track the timing of events.
     * The movementProcessor variable is declared as private and final, indicating that it cannot be reassigned once initialized.
     * The object is instantiated as a SierraTiming, which is an implementation of the Timing interface.
     */
    private final Timing movementProcessor = new SierraTiming();

    /**
     * Constructs a TimingProcessor object with the given PlayerData.
     *
     * @param playerData the PlayerData associated with the TimingProcessor
     */
    public TimingProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    /**
     * Retrieves the movement task Timing object.
     *
     * @return the Timing object for measuring the timing of the movement task
     */
    @Override
    public Timing getMovementTask() {
        return movementProcessor;
    }

    /**
     * Retrieves the Timing object for measuring the timing of the packet receive task.
     *
     * @return the Timing object for measuring the timing of the packet receive task
     *
     * @see TimingHandler#getPacketReceiveTask()
     * @see Timing
     */
    @Override
    public Timing getPacketReceiveTask() {
        return packetReceiveTiming;
    }

    /**
     * Retrieves the Timing object for measuring the timing of the packet send task.
     *
     * @return the Timing object for measuring the timing of the packet send task
     *
     * @see TimingHandler#getPacketSendTask()
     * @see Timing
     */
    @Override
    public Timing getPacketSendTask() {
        return packetSendTiming;
    }
}
