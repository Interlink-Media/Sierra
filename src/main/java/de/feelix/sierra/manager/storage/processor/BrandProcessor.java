package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierraapi.events.impl.UserBrandEvent;
import lombok.Getter;

@Getter
public class BrandProcessor {

    private final PlayerData playerData;
    private boolean hasBrand = false;

    public BrandProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    public void process(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPluginMessage(event),
                playerData::exceptionDisconnect
            );
            handleChannelMessage(wrapper.getChannelName(), wrapper.getData());
        }
    }

    private void handleChannelMessage(String channel, byte[] data) {
        if (isBrandChannel(channel)) {
            processBrandData(data);
        }
    }

    private boolean isBrandChannel(String channel) {
        return channel.equalsIgnoreCase("minecraft:brand") || channel.equals("MC|Brand");
    }

    private void processBrandData(byte[] data) {
        if (data.length > 64 || data.length == 0) {
            playerData.setBrand("sent " + data.length + " bytes as brand");
        } else if (!hasBrand) {
            String brand = new String(data, 1, data.length - 1).replace(" (Velocity)", "");
            playerData.setBrand(brand);
            hasBrand = true;
        }
        Sierra.getPlugin().getEventBus().publish(new UserBrandEvent(playerData, playerData.getBrand()));
    }
}
