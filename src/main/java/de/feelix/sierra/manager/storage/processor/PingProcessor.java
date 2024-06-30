package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import lombok.Getter;

@Getter
public class PingProcessor {

    private final PlayerData playerData;
    private long ping = -1;
    private long lastId = -1;
    private long lastTime = -1;

    public PingProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    public void handlePacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
            WrapperPlayServerKeepAlive wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayServerKeepAlive(event),
                playerData::exceptionDisconnect
            );
            this.lastId = wrapper.getId();
            this.lastTime = System.currentTimeMillis();
        }
    }

    public void handlePacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            WrapperPlayClientKeepAlive wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientKeepAlive(event),
                playerData::exceptionDisconnect
            );
            if (wrapper.getId() == this.lastId) {
                this.ping = System.currentTimeMillis() - this.lastTime;
            }
        }
    }
}
