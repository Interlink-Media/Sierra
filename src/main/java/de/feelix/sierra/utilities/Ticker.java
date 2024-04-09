package de.feelix.sierra.utilities;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.DataManager;
import de.feelix.sierra.manager.storage.PlayerData;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class Ticker {

    @Getter
    private static Ticker     instance;
    private        int        currentTick;
    private        BukkitTask task;

    /**
     * The Ticker function is a class that allows the plugin to keep track of time.
     * It does this by creating an instance of itself, and then running a task timer
     * asynchronously every tick (20 ticks per second). The Ticker function also has
     * another task timer that runs every 20 ticks (once per second) which resets the packet allowance for each
     * player. This is done so that players can't spam packets in one tick and then not send any packets for 19
     * seconds, thus bypassing our anti-spam system.
     */
    public Ticker() {
        instance = this;

        if (PacketEvents.getAPI() == null) return;

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
