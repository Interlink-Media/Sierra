package de.feelix.sierra.manager.init.impl.start;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.init.Initable;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.impl.SierraCheck;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
public class Ticker implements Initable {

    @Getter
    private static Ticker instance;

    private int currentTick;

    @Override
    public void start() {
        instance = this;
        scheduleTickTask();
        scheduleByteResetTask();
        schedulePlayerDataPollTask();
    }

    private void scheduleTickTask() {
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> currentTick++, 1, 1);
    }

    private void scheduleByteResetTask() {
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> {
            for (PlayerData value : SierraDataManager.getInstance().getPlayerData().values()) {
                value.setBytesSent(0);
            }
        }, 0, 20);
    }

    private void schedulePlayerDataPollTask() {
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> {
            for (PlayerData value : SierraDataManager.getInstance().getPlayerData().values()) {
                if (isUserValid(value)) {
                    Player player = Bukkit.getPlayer(value.getUser().getName());
                    if (player != null) {
                        value.setGameMode(GameMode.valueOf(player.getGameMode().name()));
                        value.pollData(player);
                    }
                }
                handleCheckViolations(value);
            }
        }, 5, 20);
    }

    private boolean isUserValid(PlayerData playerData) {
        return playerData.getUser() != null && playerData.getUser().getName() != null;
    }

    private void handleCheckViolations(PlayerData playerData) {
        for (SierraCheck sierraCheck : playerData.getCheckManager().availableChecks()) {
            boolean timeSinceLastDetection = System.currentTimeMillis() - sierraCheck.lastDetection() > 4000;
            if (sierraCheck.violations() > 0 && timeSinceLastDetection) {
                sierraCheck.setViolations(sierraCheck.violations() - 1);
            }
        }
    }
}
