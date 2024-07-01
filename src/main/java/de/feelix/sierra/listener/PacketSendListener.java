package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.check.impl.SierraCheck;

public class PacketSendListener extends PacketListenerAbstract {

    public PacketSendListener() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) {
            return;
        }

        PlayerData playerData = getPlayerData(event);

        if (playerData == null || handleExemptOrBlockedPlayer(playerData, event)) return;

        if (bypassPermission(playerData)) {
            event.setCancelled(false);
            return;
        }

        playerData.getTimingProcessor().getPacketSendTask().prepare();

        handleTransaction(event, playerData);

        playerData.getGameModeProcessor().process(event);
        playerData.getPingProcessor().handlePacketSend(event);

        processAvailableChecksSend(playerData, event);
        playerData.getTimingProcessor().getPacketSendTask().end();
    }

    private void handleTransaction(PacketSendEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Server.PING) {
            handlePingTransaction(event, playerData);
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            handleWindowConfirmationTransaction(event, playerData);
        }
    }

    private void handlePingTransaction(PacketSendEvent event, PlayerData playerData) {
        WrapperPlayServerPing wrapper = new WrapperPlayServerPing(event);

        int id = wrapper.getId();
        // Check if in the short range, we only use short range
        if (id == (short) id) {
            // Cast ID twice so we can use the list
            Short shortID = ((short) id);
            if (playerData.getTransactionProcessor().didWeSendThatTrans.remove(shortID)) {
                Pair<Short, Long> solarPair = new Pair<>(shortID, System.nanoTime());
                playerData.getTransactionProcessor().transactionsSent.add(solarPair);
                playerData.getTransactionProcessor().lastTransactionSent.getAndIncrement();
            }
        }
    }

    private void handleWindowConfirmationTransaction(PacketSendEvent event, PlayerData playerData) {
        WrapperPlayServerWindowConfirmation wrapper = new WrapperPlayServerWindowConfirmation(event);

        short id = wrapper.getActionId();

        // Vanilla always uses an ID starting from 1
        if (id <= 0) {

            if (playerData.getTransactionProcessor().didWeSendThatTrans.remove((Short) id)) {
                Pair<Short, Long> solarPair = new Pair<>(id, System.nanoTime());
                playerData.getTransactionProcessor().transactionsSent.add(solarPair);
                playerData.getTransactionProcessor().lastTransactionSent.getAndIncrement();
            }
        }
    }

    private void processAvailableChecksSend(PlayerData playerData, PacketSendEvent event) {
        for (SierraCheck availableCheck : playerData.getCheckManager().availableChecks()) {
            if (availableCheck instanceof OutgoingProcessor) {
                ((OutgoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }

    private boolean bypassPermission(PlayerData playerData) {
        return Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("enable-bypass-permission", false)
               && playerData.isBypassPermission();
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

    private PlayerData getPlayerData(ProtocolPacketEvent<Object> event) {
        return SierraDataManager.getInstance().getPlayerData(event.getUser()).get();
    }
}
