package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.manager.storage.logger.LogTag;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.violation.MitigationStrategy;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.logging.Logger;

public class PacketReceiveListener extends PacketListenerAbstract {

    private static final Logger LOGGER = Sierra.getPlugin().getLogger();

    public PacketReceiveListener() {
        super(PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) {
            return;
        }

        PlayerData playerData = getPlayerData(event);

        if (playerData == null) {
            event.getUser().closeConnection();
            return;
        }

        if (isWeirdPacket(event, playerData)) return;

        if (bypassPermission(playerData)) {
            event.setCancelled(false);
            return;
        }

        playerData.getTimingProcessor().getPacketReceiveTask().prepare();

        playerData.getTransactionProcessor().handleTransactionClient(event);

        handleLocale(event, playerData);

        if (handleExemptOrBlockedPlayer(playerData, event)) return;

        playerData.getBrandProcessor().process(event);
        playerData.getPingProcessor().handlePacketReceive(event);
        playerData.getCheckManager().processAvailableChecksReceive(event);

        playerData.getTimingProcessor().getPacketReceiveTask().end();
    }

    private void handleLocale(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
            WrapperPlayClientSettings wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientSettings(event), playerData::exceptionDisconnect);
            playerData.setLocale(wrapper.getLocale());
        }
    }

    private boolean isWeirdPacket(ProtocolPacketEvent event, PlayerData playerData) {

        YamlConfiguration sierraConfig = Sierra.getPlugin().getSierraConfigEngine().config();

        int readableBytes = ByteBufHelper.readableBytes(event.getByteBuf());
        int maxPacketSize = sierraConfig.getInt("generic-packet-size-limit", 6000);
        int capacity = ByteBufHelper.capacity(event.getByteBuf());

        boolean shouldCheck = maxPacketSize != -1;

        boolean isPacketTooLarge = readableBytes > maxPacketSize;
        boolean isReadableBytesGreaterThanCapacity = readableBytes > capacity;

        boolean isNegativePacketId = event.getPacketId() < 0;
        boolean isPacketIdWeird = event.getPacketId() > 1000;
        boolean isPacketSizeOrBytesWeird = shouldCheck && (isPacketTooLarge || isReadableBytesGreaterThanCapacity);

        if (isPacketSizeOrBytesWeird || isNegativePacketId || isPacketIdWeird) {

            playerData.getSierraLogger().log(LogTag.PRE, FormatUtils.chainDebugs(Arrays.asList(
                new Debug<>("Packet", event.getPacketType().getName()),
                new Debug<>("Bytes", readableBytes),
                new Debug<>("Max", maxPacketSize),
                new Debug<>("Capacity", capacity),
                new Debug<>("Packet-ID", event.getPacketId())
            )));

            logAndDisconnect(playerData, readableBytes, capacity, maxPacketSize);
            playerData.cancelEvent(event);
            playerData.punish(MitigationStrategy.KICK);
            return true;
        }
        return false;
    }

    private void logAndDisconnect(PlayerData playerData, int readableBytes, int capacity, int maxPacketSize) {
        LOGGER.info(String.format("Disconnecting %s, packet too big. Bytes: %d, capacity: %d, max: %d",
                                  playerData.getUser().getName(), readableBytes, capacity, maxPacketSize
        ));
        createHistory(playerData, readableBytes, capacity, maxPacketSize);
    }

    private void createHistory(PlayerData playerData, int readableBytes, int capacity, int maxPacketSize) {
        Sierra.getPlugin().getSierraDataManager().createMitigateHistory(
            playerData.username(),
            playerData.version(),
            MitigationStrategy.KICK,
            playerData.ping(),
            String.format("Sent: %d/Max: %d (%d)", readableBytes, maxPacketSize, capacity)
        );
    }

    private boolean bypassPermission(PlayerData playerData) {
        return Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("enable-bypass-permission", false)
               && playerData.isBypassPermission();
    }

    private PlayerData getPlayerData(ProtocolPacketEvent event) {
        return SierraDataManager.getInstance().getPlayerData(event.getUser()).get();
    }

    private boolean handleExemptOrBlockedPlayer(PlayerData playerData, ProtocolPacketEvent event) {
        if (playerData.isExempt()) {
            event.setCancelled(false);
            return true;
        }
        if (playerData.isReceivedPunishment()) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }
}
