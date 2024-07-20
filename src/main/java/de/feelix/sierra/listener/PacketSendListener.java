package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;

public class PacketSendListener extends PacketListenerAbstract {

    public PacketSendListener() {
        super(PacketListenerPriority.HIGHEST);
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

        playerData.getTransactionProcessor().handleTransactionSend(event);

        playerData.getGameModeProcessor().process(event);
        playerData.getPingProcessor().handlePacketSend(event);
        playerData.getCheckManager().processAvailableChecksSend(event);

        playerData.getTimingProcessor().getPacketSendTask().end();
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
