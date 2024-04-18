package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.user.impl.SierraUser;
import lombok.Data;

/**
 * The PingProcessor class represents a processor for handling ping-related packets in a Minecraft server.
 * It processes the packets to calculate the ping of a player.
 */
@Data
public class PingProcessor {

    /**
     * The PlayerData variable represents the data associated with a player in a Minecraft server.
     * It is used in the PingProcessor class to calculate the ping of a player.
     * <p>
     * The PlayerData class implements the SierraUser interface, which provides methods to retrieve
     * information about the player.
     * <p>
     * Usage example:
     * PingProcessor pingProcessor = new PingProcessor(playerData);
     *
     * @see SierraUser
     */
    private final PlayerData playerData;

    /**
     * The ping variable represents the ping of a player in a Minecraft server.
     */
    private long ping = -1;

    /**
     * The variable "lastId" represents the last ID of a keep-alive packet received by the PingProcessor.
     * This ID is used to match the keep-alive packets sent by the server and received by the client.
     * The value of "lastId" is initially set to -1, indicating that no keep-alive packet has been received yet.
     * Once a keep-alive packet is received, the value of "lastId" is updated with the ID of the received packet.
     * <p>
     * The purpose of tracking the last ID is to calculate the ping of a player. The server sends keep-alive packets
     * to the client and the client responds with a keep-alive packet containing the same ID. The time taken by the
     * client
     * to respond back is measured to calculate the ping. If the ID of the received keep-alive packet matches the
     * last ID,
     * the ping is calculated by subtracting the time when the keep-alive packet was received from the current time.
     * <p>
     * Example usage:
     * PingProcessor pingProcessor = new PingProcessor(playerData);
     * long receivedId = pingProcessor.lastId;
     */
    private long lastId = -1;

    /**
     * Represents a timestamp indicating the last time a certain event occurred.
     */
    private long lastTime = -1;

    /**
     * The PingProcessor class represents a processor for handling ping-related packets in a Minecraft server.
     * It processes the packets to calculate the ping of a player.
     */
    public PingProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    /**
     * The PingProcessor class represents a processor for handling ping-related packets in a Minecraft server.
     * It processes the packets to calculate the ping of a player.
     */
    public void handle(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
            WrapperPlayServerKeepAlive wrapper = new WrapperPlayServerKeepAlive(event);

            this.lastId = wrapper.getId();
            this.lastTime = System.currentTimeMillis();
        }
    }

    /**
     * This method handles the PacketReceiveEvent and processes the packet to calculate the ping of a player.
     * If the packet type is KEEP_ALIVE, it retrieves the ID from the packet and compares it with the last ID received.
     * If they match, it calculates the ping by subtracting the last time from the current time.
     *
     * @param event The PacketReceiveEvent representing the received packet.
     */
    public void handle(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            WrapperPlayClientKeepAlive wrapper = new WrapperPlayClientKeepAlive(event);

            if (wrapper.getId() == this.lastId) {
                this.ping = System.currentTimeMillis() - this.lastTime;
            }
        }
    }
}
