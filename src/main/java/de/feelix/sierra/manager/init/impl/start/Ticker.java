package de.feelix.sierra.manager.init.impl.start;

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
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> currentTick++, 1, 1);

        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> {
            double maxPacketAllowance = 1000 * 2;

            for (PlayerData value : SierraDataManager.getInstance().getPlayerData().values()) {
                value.setPacketAllowance(maxPacketAllowance);
                value.setPacketCount(0);
                value.setBytesSent(0);

                if (value.getUser().getName() != null) {
                    Player player = Bukkit.getPlayer(value.getUser().getName());
                    if (player != null) {
                        Sierra.getPlugin().getSierraDataManager().setPlayerGameMode(value, player);
                        value.pollData(player);
                    }
                }

                for (SierraCheck sierraCheck : value.getCheckManager().availableChecks()) {
                    if (sierraCheck.violations() > 0) {
                        sierraCheck.setViolations(sierraCheck.violations() - 1);
                    }
                }
            }
        }, 0, 20);
    }
}
