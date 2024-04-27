package de.feelix.sierra.manager.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import de.feelix.sierra.manager.storage.PlayerData;

/**
 * The OutgoingProcessor interface represents a processor for handling outgoing packets.
 */
public interface OutgoingProcessor {

    /**
     * The handle method is responsible for handling a PacketSendEvent.
     * It takes in a PacketSendEvent object representing the packet send event and a PlayerData object representing the player's data.
     *
     * @param event       The PacketSendEvent object representing the packet send event
     * @param playerData  The PlayerData object representing the player's data
     */
    void handle(PacketSendEvent event, PlayerData playerData);

}
