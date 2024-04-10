package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.DataManager;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.impl.SierraCheck;

import java.util.logging.Level;

public class PacketListener extends PacketListenerAbstract {

    public PacketListener() {
        super(PacketListenerPriority.LOWEST);
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

        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            handleChannelMessage(playerData, wrapper.getChannelName(), wrapper.getData());
        }

        for (SierraCheck availableCheck : playerData.getCheckManager().availableChecks()) {
            if (availableCheck instanceof IngoingProcessor) {
                ((IngoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }

    private void handleChannelMessage(PlayerData playerData, String channel, byte[] data) {
        if (channel.equalsIgnoreCase("minecraft:brand") || // 1.13+
            channel.equals("MC|Brand")) { // 1.12
            if (data.length > 64 || data.length == 0) {
                playerData.setBrand("sent " + data.length + " bytes as brand");
            } else if (!playerData.isHasBrand()) {

                byte[] minusLength = new byte[data.length - 1];
                System.arraycopy(data, 1, minusLength, 0, minusLength.length);

                // Removes velocity's brand suffix
                playerData.setBrand(new String(minusLength).replace(" (Velocity)", ""));
            }
            playerData.setHasBrand(true);
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

        PacketTypeCommon typeCommon = event.getPacketType();

        if (typeCommon == PacketType.Play.Server.CHANGE_GAME_STATE) {
            WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);

            if (packet.getReason() == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) {

                int gameMode = (int) packet.getValue();

                // Some plugins send invalid values such as -1, this is what the client does
                if (gameMode < 0 || gameMode >= GameMode.values().length) {
                    playerData.setGameMode(GameMode.SURVIVAL);
                } else {
                    playerData.setGameMode(GameMode.values()[gameMode]);
                }
            }
        }

        if (typeCommon == PacketType.Play.Server.JOIN_GAME) {
            WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(event);
            playerData.setGameMode(joinGame.getGameMode());
        }

        if (typeCommon == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn respawn = new WrapperPlayServerRespawn(event);
            playerData.setGameMode(respawn.getGameMode());
        }

        for (SierraCheck availableCheck : playerData.getCheckManager().availableChecks()) {
            if (availableCheck instanceof OutgoingProcessor) {
                ((OutgoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }
}
