package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import de.feelix.sierra.manager.storage.PlayerData;
import lombok.Getter;

@Getter
public class BrandProcessor {

    private final PlayerData playerData;

    public BrandProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    public void process(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            handleChannelMessage(playerData, wrapper.getChannelName(), wrapper.getData());
        }
    }

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
