package de.feelix.sierra.utilities;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.DataManager;
import de.feelix.sierra.manager.storage.PlayerData;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * The {@code Ticker} class represents a timer that runs asynchronously and performs tasks at regular intervals.
 * {@code Ticker} is a singleton and can be accessed using the {@code instance} variable.
 * It maintains a {@code currentTick} count that increments by 1 every tick.
 * It also has a {@code task} that runs every second and performs certain actions.
 */
@Getter
public class Ticker {

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
     * @see Ticker#task
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

    /**
     * This private final variable represents a Bukkit task.
     * It holds a reference to the task created in the Ticker class which runs every second and performs certain
     * actions.
     *
     * @see Ticker
     */
    private final BukkitTask task;

    /**
     * Ticker class represents a timer that runs asynchronously and performs tasks at regular intervals.
     */
    public Ticker() {
        instance = this;
        Bukkit.getScheduler().runTaskTimerAsynchronously(Sierra.getPlugin(), () -> currentTick++, 1, 1);

        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(Sierra.getPlugin(), () -> {
            double maxPacketsPerSecond = 1000;
            double maxPacketAllowance  = maxPacketsPerSecond * 2;

            for (PlayerData value : DataManager.getInstance().getPlayerData().values()) {
                value.setPacketAllowance(maxPacketAllowance);
                value.setPacketCount(0);
                value.setBytesSent(0);
            }
        }, 0, 20);
    }
}
