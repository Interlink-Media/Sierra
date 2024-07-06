package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.manager.storage.logger.LogTag;
import de.feelix.sierraapi.check.impl.SierraCheck;
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

        handleTransaction(event, playerData);
        handleLocale(event, playerData);

        if (handleExemptOrBlockedPlayer(playerData, event)) return;

        playerData.getBrandProcessor().process(event);
        playerData.getPingProcessor().handlePacketReceive(event);

        processAvailableChecksReceive(playerData, event);
        playerData.getTimingProcessor().getPacketReceiveTask().end();
    }

    private void handleLocale(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
            WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);
            playerData.setLocale(wrapper.getLocale());
        }
    }

    private void handleTransaction(PacketReceiveEvent event, PlayerData playerData) {
        PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            handleWindowConfirmation(event, playerData);
        } else if (packetType == PacketType.Play.Client.PONG) {
            handlePong(event, playerData);
        }
    }

    private void handleWindowConfirmation(PacketReceiveEvent event, PlayerData playerData) {
        WrapperPlayClientWindowConfirmation wrapper = new WrapperPlayClientWindowConfirmation(event);
        short                               id      = wrapper.getActionId();
        if (id <= 0 && playerData.getTransactionProcessor().addTransactionResponse(id)) {
            event.setCancelled(true);
        }
    }

    private void handlePong(PacketReceiveEvent event, PlayerData playerData) {
        WrapperPlayClientPong wrapper = new WrapperPlayClientPong(event);
        int                   id      = wrapper.getId();
        if (id == (short) id && playerData.getTransactionProcessor().addTransactionResponse((short) id)) {
            event.setCancelled(true);
        }
    }

    private boolean isWeirdPacket(ProtocolPacketEvent<Object> event, PlayerData playerData) {
        int readableBytes = ByteBufHelper.readableBytes(event.getByteBuf());
        int maxPacketSize = Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getInt("generic-packet-size-limit", 5000);
        int capacity = ByteBufHelper.capacity(event.getByteBuf());

        if ((maxPacketSize != -1 && (readableBytes > maxPacketSize || readableBytes > capacity)) ||
            event.getPacketId() < 0 || event.getPacketId() > 1000) {

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

    private void processAvailableChecksReceive(PlayerData playerData, PacketReceiveEvent event) {
        for (SierraCheck availableCheck : playerData.getCheckManager().availableChecks()) {
            if (availableCheck instanceof IngoingProcessor) {
                ((IngoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }
}
