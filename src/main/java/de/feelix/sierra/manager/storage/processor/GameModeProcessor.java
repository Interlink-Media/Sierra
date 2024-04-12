package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import de.feelix.sierra.manager.storage.PlayerData;
import lombok.Getter;

@Getter
public class GameModeProcessor {

    /**
     * The playerData variable represents the data of a player in the game.
     */
    private final PlayerData playerData;

    /**
     * The GameModeProcessor class processes different packet events related to the game mode of a player.
     */
    public GameModeProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    /**
     * The process method processes different packet events related to the game mode of a player.
     *
     * @param event The PacketSendEvent representing the packet send event
     */
    public void process(PacketSendEvent event) {
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
    }
}
