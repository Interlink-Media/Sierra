package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import de.feelix.sierra.manager.storage.PlayerData;
import lombok.Getter;

@Getter
public class BrandProcessor {

    /**
     * The playerData variable represents the data of a player.
     * It is an instance of the PlayerData class, which stores information about the player's state and attributes.
     * <p>
     * The PlayerData class is used for managing plugin messages related to the player's brand.
     * It processes packet events and extracts brand information from the received messages.
     */
    private final PlayerData playerData;

    /**
     * The BrandProcessor class represents a processor that handles plugin messages related to the player's brand.
     * It processes packet events and extracts brand information from the received messages.
     */
    public BrandProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    /**
     * Processes the received packet event to handle plugin messages related to the player's brand.
     * This method is called whenever a packet is received by the server.
     *
     * @param event The PacketReceiveEvent object representing the received packet event
     */
    public void process(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            handleChannelMessage(playerData, wrapper.getChannelName(), wrapper.getData());
        }
    }

    /**
     * Handles a plugin message received on a specific channel.
     *
     * @param playerData the PlayerData object associated with the player receiving the message
     * @param channel    the channel on which the message was received
     * @param data       the data of the message
     */
    private void handleChannelMessage(PlayerData playerData, String channel, byte[] data) {
        if (channel.equalsIgnoreCase("minecraft:brand") || // 1.13+
            channel.equals("MC|Brand")) { // 1.12
            if (data.length > 64 || data.length == 0) {
                playerData.setBrand("sent " + data.length + " bytes as brand");
            } else if (!playerData.isHasBrand()) {

                byte[] minusLength = new byte[data.length - 1];
                System.arraycopy(data, 1, minusLength, 0, minusLength.length);

                // Removes velocity's brand suffix
                playerData.setBrand(new String(minusLength).replace(" (Velocity)", ""));
            }
            playerData.setHasBrand(true);
        }
    }
}
