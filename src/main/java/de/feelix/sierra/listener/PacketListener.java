package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.*;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.impl.SierraCheck;

import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * The PacketListener class represents a packet listener that receives and sends packets events.
 */
public class PacketListener extends PacketListenerAbstract {

    /**
     * A class representing a packet listener.
     */
    public PacketListener() {
        super(PacketListenerPriority.MONITOR);
    }

    /**
     * Called when a packet is received.
     *
     * @param event the PacketReceiveEvent representing the packet receive event
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        PlayerData playerData = getPlayerData(event);

        if (playerData == null) {
            disconnectUninitializedPlayer(event);
            return;
        }

        playerData.getTimingProcessor().getPacketReceiveTask().prepare();

        checkHandling(playerData, event);

        if (handleExemptOrBlockedPlayer(playerData, event)) return;

        playerData.getBrandProcessor().process(event);
        playerData.getPingProcessor().handle(event);

        processAvailableChecksReceive(playerData, event);
        playerData.getTimingProcessor().getPacketReceiveTask().end();
    }

    /**
     * Checks the handling of a packet for the given player data and event.
     * If the player data is not null, has a valid user, and the name has not been checked yet,
     * it validates the username of the event's user. If the username is invalid, it cancels the event and kicks the player.
     * Finally, it sets the nameChecked flag in the player data to true.
     *
     * @param playerData the PlayerData object representing the data associated with the player
     * @param event the PacketReceiveEvent representing the packet receive event
     */
    private void checkHandling(PlayerData playerData, PacketReceiveEvent event) {
        if (playerData != null
            && playerData.getUser() != null
            && playerData.getUser().getName() != null
            && !playerData.isNameChecked()) {
            if (!isValidUsername(event.getUser().getName())) {
                event.setCancelled(true);
                playerData.kick();
            }
            playerData.setNameChecked(true);
        }
    }

    /**
     * Called when a packet is sent.
     *
     * @param event the PacketSendEvent representing the event
     */
    @Override
    public void onPacketSend(PacketSendEvent event) {
        PlayerData playerData = getPlayerData(event);

        if (playerData == null || handleExemptOrBlockedPlayer(playerData, event)) return;

        playerData.getTimingProcessor().getPacketSendTask().prepare();

        playerData.getGameModeProcessor().process(event);
        playerData.getPingProcessor().handle(event);
        processAvailableChecksSend(playerData, event);
        playerData.getTimingProcessor().getPacketSendTask().end();
    }

    /**
     * Checks if a username is valid.
     *
     * @param username The username to be checked.
     * @return true if the username is valid, false otherwise.
     */
    public static boolean isValidUsername(String username) {
        String pattern = "^[a-zA-Z0-9_\\-.]{3,16}$";
        return Pattern.matches(pattern, username);
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

    /**
     * Disconnects an uninitialized player.
     * <p>
     * This method is called when a packet receive event is triggered and the player's data is uninitialized.
     * It logs a warning message indicating that the player is being disconnected because the packet reader is not
     * injected yet,
     * and then closes the connection of the player.
     *
     * @param event the PacketReceiveEvent representing the packet receive event
     */
    private void disconnectUninitializedPlayer(PacketReceiveEvent event) {
        String format     = "Disconnecting %s for cause packet reader is not injected yet";
        String disconnect = String.format(format, event.getUser().getName());
        Sierra.getPlugin().getLogger().log(Level.WARNING, disconnect);
        event.getUser().closeConnection();
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
     * Process the available checks for packet receive events.
     *
     * @param playerData The PlayerData object associated with the player
     * @param event      The PacketReceiveEvent object representing the packet receive event
     */
    private void processAvailableChecksReceive(PlayerData playerData, PacketReceiveEvent event) {
        for (SierraCheck availableCheck : playerData.getCheckManager().availableChecks()) {
            if (availableCheck instanceof IngoingProcessor) {
                ((IngoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }
}
