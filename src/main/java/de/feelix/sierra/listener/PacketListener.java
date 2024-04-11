package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.DataManager;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.impl.SierraCheck;

import java.util.logging.Level;

public class PacketListener extends PacketListenerAbstract {

    public PacketListener() {
        super(PacketListenerPriority.MONITOR);
    }

    /**
     * The onPacketReceive function is called whenever a player receives a packet.
     * This function will check if the player is exempt from checks, and if they are not, it will run all of their
     * checks on the packet.
     *
     * @param event event Get the user and packet
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        PlayerData playerData = DataManager.getInstance().getPlayerData(event.getUser()).get();

        if (playerData == null) {
            String msg = "Disconnecting " + event.getUser().getName() + " for cause packet reader is not injected yet";
            Sierra.getPlugin().getLogger().log(Level.WARNING, msg);
            event.getUser().closeConnection();
            return;
        }

        if (playerData.isExempt()) {
            event.setCancelled(false);
            return;
        }

        if (playerData.isBlocked()) {
            event.setCancelled(true);
            return;
        }

        playerData.getBrandProcessor().process(event);

        for (SierraCheck availableCheck : playerData.getCheckManager().availableChecks()) {
            if (availableCheck instanceof IngoingProcessor) {
                ((IngoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }

    /**
     * The onPacketSend function is called whenever a packet is sent from the server to the client.
     * This function will check if the player has been flagged as exempt, and if so, it will cancel
     * any checks that would otherwise be performed on this packet. If not, it will then check whether
     * or not they have been blocked by Sierra (i.e., are in violation of one or more checks). If
     * they are blocked, then their packets are cancelled; otherwise we proceed with checking for violations.
     *
     * @param event event Get the packet that is being sent
     */
    @Override
    public void onPacketSend(PacketSendEvent event) {

        PlayerData playerData = DataManager.getInstance().getPlayerData(event.getUser()).get();

        if (playerData == null) {
            return;
        }

        if (playerData.isExempt()) {
            event.setCancelled(false);
            return;
        }

        if (playerData.isBlocked()) {
            event.setCancelled(true);
            return;
        }

        playerData.getGameModeProcessor().process(event);

        for (SierraCheck availableCheck : playerData.getCheckManager().availableChecks()) {
            if (availableCheck instanceof OutgoingProcessor) {
                ((OutgoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }
}
