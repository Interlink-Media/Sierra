package de.feelix.sierra.manager.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import de.feelix.sierra.manager.storage.PlayerData;

public interface OutgoingProcessor {

    void handle(PacketSendEvent event, PlayerData playerData);

}
