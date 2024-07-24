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

/**
 * The {@code Ticker} class represents a timer that runs asynchronously and performs tasks at regular intervals.
 * {@code Ticker} is a singleton and can be accessed using the {@code instance} variable.
 * It maintains a {@code currentTick} count that increments by 1 every tick.
 * It also has a {@code task} that runs every second and performs certain actions.
 */
@Getter
public class Ticker implements Initable {

    /**
     * The {@code Ticker} class represents a timer that runs asynchronously and performs tasks at regular intervals.
     * {@code Ticker} is a singleton and can be accessed using the {@code instance} variable.
     * It maintains a {@code currentTick} count that increments by 1 every tick.
     * It also has a {@code task} that runs every second and performs certain actions.
     *
     * <p>
     * The {@code Ticker} class is part of the {@code Sierra} plugin and can be obtained through the {@code Sierra}
     * class.
     * </p>
     *
     * @see Sierra#getPlugin()
     * @see Ticker#currentTick
     */
    @Getter
    private static Ticker instance;

    /**
     * Represents the current tick count of the ticker.
     * The tick count increments by 1 every tick.
     *
     * @see Ticker
     */
    private int currentTick;

    @Override
    public void start() {
        instance = this;

        // Ticker task
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> currentTick++,
                                                          1, 1
        );

        // Each second reset player bytes
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> {
            for (PlayerData value : SierraDataManager.getInstance().getPlayerData().values()) {
                value.setBytesSent(0);
            }
        }, 0, 20);

        // Poll data thread and violation reset thread
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> {
            for (PlayerData value : SierraDataManager.getInstance().getPlayerData().values()) {

                boolean isUserValid = value.getUser() != null && value.getUser().getName() != null;

                if (isUserValid) {
                    Player player = Bukkit.getPlayer(value.getUser().getName());
                    if (player != null) {
                        value.setGameMode(GameMode.valueOf(player.getGameMode().name()));
                        value.pollData(player);
                    }
                }

                for (SierraCheck sierraCheck : value.getCheckManager().availableChecks()) {
                    boolean timeSinceLastDetection = System.currentTimeMillis() - sierraCheck.lastDetection() > 4000;

                    if (sierraCheck.violations() > 0 && timeSinceLastDetection) {
                        sierraCheck.setViolations(sierraCheck.violations() - 1);
                    }
                }
            }
        }, 0, 2);
    }
}
