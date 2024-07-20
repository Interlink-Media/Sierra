package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.manager.storage.logger.LogTag;
import de.feelix.sierraapi.violation.MitigationStrategy;

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

        if (playerData.getClientVersion() == null) {
            playerData.setClientVersion(event.getUser().getClientVersion());
        }

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
            WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);
            playerData.setLocale(wrapper.getLocale());
        }
    }

    private boolean isWeirdPacket(ProtocolPacketEvent<Object> event, PlayerData playerData) {

        int readableBytes = ByteBufHelper.readableBytes(event.getByteBuf());

        int maxPacketSize = Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getInt("generic-packet-size-limit", 6000);

        int capacity = ByteBufHelper.capacity(event.getByteBuf());

        boolean shouldCheck = maxPacketSize != -1;

        boolean isPacketTooLarge = readableBytes > maxPacketSize;
        boolean isReadableBytesGreaterThanCapacity = readableBytes > capacity;

        boolean isNegativePacketId = event.getPacketId() < 0;
        boolean isPacketIdWeird = event.getPacketId() > 1000;

        if ((shouldCheck && (isPacketTooLarge || isReadableBytesGreaterThanCapacity))
            || isNegativePacketId || isPacketIdWeird) {

            playerData.getSierraLogger()
                .log(
                    LogTag.PRE, String.format("Packet: %s, Bytes: %d (Max: %d, Capacity: %d) Packet-Id: %d",
                                              event.getPacketType().getName(), readableBytes, maxPacketSize, capacity,
                                              event.getPacketId()
                    ));

            logAndDisconnect(playerData, readableBytes, capacity, maxPacketSize);
            event.cleanUp();
            event.setCancelled(true);
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

    private PlayerData getPlayerData(ProtocolPacketEvent<Object> event) {
        return SierraDataManager.getInstance().getPlayerData(event.getUser()).get();
    }

    private boolean handleExemptOrBlockedPlayer(PlayerData playerData, ProtocolPacketEvent<?> event) {
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
