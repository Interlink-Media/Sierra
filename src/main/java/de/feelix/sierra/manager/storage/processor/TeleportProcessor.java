package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Teleport;
import lombok.Getter;

@Getter
public class TeleportProcessor {

    private final PlayerData playerData;
    private Teleport teleport;

    public TeleportProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    public void handle(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {

            WrapperPlayServerPlayerPositionAndLook wrapper = new WrapperPlayServerPlayerPositionAndLook(event);

            this.teleport = new Teleport(
                wrapper.getTeleportId(),
                new Vector3d(wrapper.getX(), wrapper.getY(), wrapper.getZ()),
                wrapper.getYaw(),
                wrapper.getPitch()
            );
        }
    }

    public long getLastTeleportTime() {
        return this.teleport != null ? teleport.getTimestamp() : 0;
    }
}
