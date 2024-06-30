package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import lombok.Getter;

@Getter
public class GameModeProcessor {

    private final PlayerData playerData;

    public GameModeProcessor(PlayerData playerData) {
        this.playerData = playerData;
    }

    public void process(PacketSendEvent event) {
        PacketTypeCommon typeCommon = event.getPacketType();

        if (typeCommon.equals(PacketType.Play.Server.CHANGE_GAME_STATE)) {
            handleGameStateChange(event);
        } else if (typeCommon.equals(PacketType.Play.Server.JOIN_GAME)) {
            handleJoinGame(event);
        } else if (typeCommon.equals(PacketType.Play.Server.RESPAWN)) {
            handleRespawn(event);
        }
    }

    private void handleGameStateChange(PacketSendEvent event) {
        WrapperPlayServerChangeGameState packet = CastUtil.getSupplier(
            () -> new WrapperPlayServerChangeGameState(event),
            playerData::exceptionDisconnect
        );

        if (packet.getReason() == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) {
            int gameModeValue = (int) packet.getValue();

            // Set default to SURVIVAL if the value is out of bounds
            GameMode gameMode = (gameModeValue < 0 || gameModeValue >= GameMode.values().length) ?
                GameMode.SURVIVAL :
                GameMode.values()[gameModeValue];

            playerData.setGameMode(gameMode);
        }
    }

    private void handleJoinGame(PacketSendEvent event) {
        WrapperPlayServerJoinGame joinGame = CastUtil.getSupplier(
            () -> new WrapperPlayServerJoinGame(event),
            playerData::exceptionDisconnect
        );
        playerData.setGameMode(joinGame.getGameMode());
    }

    private void handleRespawn(PacketSendEvent event) {
        WrapperPlayServerRespawn respawn = CastUtil.getSupplier(
            () -> new WrapperPlayServerRespawn(event),
            playerData::exceptionDisconnect
        );
        playerData.setGameMode(respawn.getGameMode());
    }
}
