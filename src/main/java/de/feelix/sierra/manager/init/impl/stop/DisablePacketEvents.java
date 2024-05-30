package de.feelix.sierra.manager.init.impl.stop;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.manager.init.Initable;

/**
 * The DisablePacketEvents class implements the Initable interface.
 * It is responsible for terminating the PacketEvents API if it is running.
 *
 * @see Initable
 */
public class DisablePacketEvents implements Initable {

    /**
     * Terminate the PacketEvents API if it is running.
     */
    @Override
    public void start() {
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
    }
}
