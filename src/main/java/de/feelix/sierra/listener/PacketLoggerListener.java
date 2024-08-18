package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.manager.storage.logger.LogTag;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.FormatUtils;

public class PacketLoggerListener extends PacketListenerAbstract {

    public PacketLoggerListener() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        PlayerData playerData = getPlayerData(event);

        if (playerData == null) return;

        if (playerData.isReceivedPunishment() || event.isCancelled() || playerData.isExempt()) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {

            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPlayerBlockPlacement(event), playerData::exceptionDisconnect);

            wrapper.getItemStack().ifPresent(itemStack -> {
                if (itemStack.getNBT() != null) {
                    playerData.getSierraLogger()
                        .log(LogTag.BLOCK_PLACE, FormatUtils.mapToString(itemStack.getNBT().getTags()));
                }
            });

        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {

            WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPluginMessage(event), playerData::exceptionDisconnect);

            String payload = wrapper.getChannelName();

            if (payload.contains("MC|BEdit") || payload.contains("MC|BSign") || payload.contains("MC|BOpen")) {

                Object buffer = null;
                try {
                    buffer = UnpooledByteBufAllocationHelper.buffer();
                    ByteBufHelper.writeBytes(buffer, wrapper.getData());
                    PacketWrapper<?> universalWrapper = PacketWrapper.createUniversalPacketWrapper(buffer);

                    try {
                        ItemStack itemStack = universalWrapper.readItemStack();

                        if (itemStack.getNBT() != null) {
                            playerData.getSierraLogger()
                                .log(
                                    LogTag.PAYLOAD, FormatUtils.mapToString(itemStack.getNBT().getTags())
                                );
                        }
                    } catch (Exception exception) {
                        playerData.getSierraLogger().log(LogTag.EXCEP_PAYLOAD, exception.getMessage());
                    }
                } finally {
                    ByteBufHelper.release(buffer);
                }
            }

        } else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {

            WrapperPlayClientCreativeInventoryAction wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientCreativeInventoryAction(event), playerData::exceptionDisconnect);

            ItemStack itemStack = wrapper.getItemStack();
            if (itemStack != null && itemStack.getNBT() != null) {
                playerData.getSierraLogger()
                    .log(LogTag.CREATIVE, FormatUtils.mapToString(itemStack.getNBT().getTags()));
            }

        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {

            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindow(event), playerData::exceptionDisconnect);

            ItemStack itemStack = wrapper.getCarriedItemStack();
            if (itemStack != null && itemStack.getNBT() != null) {
                playerData.getSierraLogger()
                    .log(LogTag.WINDOW_CLICK, FormatUtils.mapToString(itemStack.getNBT().getTags()));
            }
        }
    }

    private PlayerData getPlayerData(ProtocolPacketEvent event) {
        return SierraDataManager.getInstance().getPlayerData(event.getUser()).get();
    }
}
