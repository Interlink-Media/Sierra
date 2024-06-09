package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierraapi.check.impl.SierraCheck;
import org.bukkit.entity.Player;

import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The PacketReceiveListener class is a subclass of PacketListenerAbstract and implements the PacketListener interface.
 * It listens for received packets and performs various checks and operations on them.
 */
public class PacketReceiveListener extends PacketListenerAbstract {

    /**
     * The PacketReceiveListener class is a subclass of PacketListenerAbstract and implements
     * the PacketListener interface. It represents a listener for packet receive events.
     * <p>
     * This listener is responsible for handling the packet receive events triggered
     * by the PacketEvents API. It provides a callback method called onPacketReceive()
     * which is called whenever a packet is received.
     * <p>
     * Example usage:
     * PacketEvents.getAPI().getEventManager().registerListeners(new PacketReceiveListener(), new PacketSendListener());
     * PacketEvents.getAPI().init();
     */
    public PacketReceiveListener() {
        super(PacketListenerPriority.MONITOR);
    }

    /**
     * Called when a packet is received.
     *
     * @param event the PacketReceiveEvent representing the packet receive event
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        if (event.getConnectionState() != ConnectionState.PLAY) {
            return;
        }

        PlayerData playerData = getPlayerData(event);

        if (playerData == null) {
            disconnectUninitializedPlayer(event);
            return;
        }

        if (weirdPacket(event, playerData)) return;

        if (bypassPermission(event)) {
            event.setCancelled(false);
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
     * Checks if a received packet is considered weird based on certain conditions. If the packet is considered weird,
     * it cancels the event, performs necessary actions such as kicking the player, and returns true. Otherwise, it
     * returns false.
     *
     * @param event      the ProtocolPacketEvent representing the packet receive event
     * @param playerData the PlayerData object associated with the player
     * @return true if the packet is considered weird, false otherwise
     */
    private boolean weirdPacket(ProtocolPacketEvent<Object> event, PlayerData playerData) {
        int    readableBytes = ByteBufHelper.readableBytes(event.getByteBuf());
        Logger logger        = Sierra.getPlugin().getLogger();

        int maxPacketSize = Sierra.getPlugin().getSierraConfigEngine().config().getInt(
            "generic-packet-size-limit", 5000);

        if (maxPacketSize != -1 && readableBytes > maxPacketSize) {
            logger.severe("Disconnecting " + playerData.getUser().getName() + ", because packet is too big.");
            logger.severe("If this is a false kick, increase the generic-packet-size-limit to " + readableBytes + 100);
            event.setCancelled(true);
            playerData.kick();
            return true;
        }

        if (event.getPacketId() < 0 || event.getPacketId() > 1000) {
            logger.severe("Disconnecting " + playerData.getUser().getName() + ", because packet id is invalid");
            event.setCancelled(true);
            playerData.kick();
            return true;
        }

        long time = System.nanoTime();

        try {
            //noinspection unused
            PacketWrapper<?> universalPacketWrapper = PacketWrapper.createUniversalPacketWrapper(event.getByteBuf());
        } catch (Exception exception) {
            logger.severe("Disconnecting " + playerData.getUser().getName() + ", because packet cant be processed");
            event.setCancelled(true);
            playerData.exceptionDisconnect(exception);
            return true;
        }
        long duration = System.nanoTime() - time;

        if (duration > 1000000000L) {
            logger.severe("Disconnecting " + playerData.getUser().getName() + ", because processing time was to big");
            event.setCancelled(true);
            playerData.kick();
            return true;
        }
        return false;
    }

    /**
     * Checks if the player has bypass permission.
     *
     * @param event the ProtocolPacketEvent representing the event
     * @return true if the player has bypass permission, false otherwise
     */
    private boolean bypassPermission(ProtocolPacketEvent<Object> event) {
        if (Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("enable-bypass-permission", false)) {
            Player player = (Player) event.getPlayer();
            if (player != null) {
                return player.hasPermission("sierra.bypass");
            }
        }
        return false;
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
        Sierra.getPlugin().getLogger().warning(disconnect);
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

    /**
     * Checks the handling of a packet for the given player data and event.
     * If the player data is not null, has a valid user, and the name has not been checked yet,
     * it validates the username of the event's user. If the username is protocol, it cancels the event and kicks the
     * player.
     * Finally, it sets the nameChecked flag in the player data to true.
     *
     * @param playerData the PlayerData object representing the data associated with the player
     * @param event      the PacketReceiveEvent representing the packet receive event
     */
    private void checkHandling(PlayerData playerData, PacketReceiveEvent event) {
        if (playerData != null
            && playerData.getUser() != null
            && playerData.getUser().getName() != null
            && !playerData.isNameChecked()) {
            if (!isValidUsername(event.getUser().getName())) {
                Sierra.getPlugin()
                    .getLogger()
                    .info("Invalid username: " + event.getUser().getName() + ", kicked player");
                event.setCancelled(true);
                playerData.kick();
            }
            playerData.setNameChecked(true);
        }
    }
}
