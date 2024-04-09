package de.feelix.sierra.manager.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import de.feelix.sierra.manager.storage.PlayerData;

public interface IngoingProcessor {

    void handle(PacketReceiveEvent event, PlayerData playerData);
}
