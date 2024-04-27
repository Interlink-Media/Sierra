package de.feelix.sierra.manager.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import de.feelix.sierra.manager.storage.PlayerData;

/**
 * The IngoingProcessor interface represents a processor for handling incoming packets.
 */
public interface IngoingProcessor {

    /**
     * The handle method is called to process a received packet event and perform necessary actions based on the player data.
     *
     * @param event      The PacketReceiveEvent object representing the received packet event
     * @param playerData The PlayerData object representing the data of the player who received the packet
     */
    void handle(PacketReceiveEvent event, PlayerData playerData);
}
