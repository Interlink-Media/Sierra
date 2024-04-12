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

    /**
     * The PacketListener class is a packet listener that extends the PacketListenerAbstract class.
     * It is used to listen for packets being sent and received by the server.
     */
    public PacketListener() {
        super(PacketListenerPriority.MONITOR);
    }


    /**
     * The onPacketReceive method is called whenever a packet is received by the server.
     * It processes the received packet, checks for exemptions and blocks, and handles
     * the packet based on the available checks.
     *
     * @param event The PacketReceiveEvent object representing the received packet event
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
     * The onPacketSend method is called when a packet is about to be sent by the server.
     * It processes the packet, checks for exemptions and blocks, and handles the packet based on available checks.
     *
     * @param event The PacketSendEvent object representing the packet send event
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
