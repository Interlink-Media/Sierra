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

/**
 * A packet listener that listens for packet send events and performs necessary processing
 */
public class PacketSendListener extends PacketListenerAbstract {

    /**
     * PacketSendListener is a class that extends PacketListenerAbstract and represents a listener for packet send
     * events.
     * It is used to handle actions and logic when a packet is sent.
     */
    public PacketSendListener() {
        super(PacketListenerPriority.MONITOR);
    }

    /**
     * Called when a packet is sent.
     *
     * @param event the PacketSendEvent representing the event
     */
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
        playerData.getPingProcessor().handle(event);

        processAvailableChecksSend(playerData, event);
        playerData.getTimingProcessor().getPacketSendTask().end();
    }

    private void handleTransaction(PacketSendEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Server.PING) {
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

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
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
    }

    /**
     * Process the available checks for packet send events.
     *
     * @param playerData The PlayerData object associated with the player
     * @param event      The PacketSendEvent object representing the packet send event
     */
    private void processAvailableChecksSend(PlayerData playerData, PacketSendEvent event) {
        for (SierraCheck availableCheck : playerData.getCheckManager().availableChecks()) {
            if (availableCheck instanceof OutgoingProcessor) {
                ((OutgoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }

    /**
     * Checks whether bypass permission is enabled and if the player has the bypass permission.
     *
     * @param playerData The PlayerData object associated with the player
     * @return true if bypass permission is enabled and the player has the bypass permission, false otherwise
     */
    private boolean bypassPermission(PlayerData playerData) {
        if (Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("enable-bypass-permission", false)) {
            return playerData.isBypassPermission();
        }
        return false;
    }

    /**
     * Handles exemption or blocking of a player.
     *
     * @param playerData The PlayerData object associated with the player
     * @param event      The ProtocolPacketEvent representing the event
     * @return true if the player is exempt or blocked, false otherwise
     */
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

    /**
     * Retrieves the PlayerData object associated with a given ProtocolPacketEvent.
     *
     * @param event the ProtocolPacketEvent representing the event
     * @return the PlayerData object associated with the event
     */
    private PlayerData getPlayerData(ProtocolPacketEvent<Object> event) {
        return SierraDataManager.getInstance().getPlayerData(event.getUser()).get();
    }
}
