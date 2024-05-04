package de.feelix.sierra.command.impl;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.commands.*;
import de.feelix.sierraapi.timing.Timing;
import de.feelix.sierraapi.timing.TimingHandler;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * MonitorCommand is a class that represents a command that prints monitoring information related to the player.
 * It implements the ISierraCommand interface, which defines the necessary methods that need to be implemented by a
 * command class.
 */
public class MonitorCommand implements ISierraCommand {

    /**
     * This method processes the command by printing monitoring information related to the player.
     *
     * @param sierraSender    The ISierraSender object representing the sender of the command.
     * @param abstractCommand The IBukkitAbstractCommand object representing the abstract command.
     * @param sierraLabel     The ISierraLabel object representing the label of the initial symbol.
     * @param sierraArguments The ISierraArguments object representing the arguments passed with the command.
     */
    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand, ISierraLabel sierraLabel,
                        ISierraArguments sierraArguments) {

        if (sierraSender.getSenderAsPlayer() == null) return;

        Player player = sierraSender.getSenderAsPlayer();

        player.sendMessage(Sierra.PREFIX + " §fPerformance monitor §7(Your data)");

        WeakReference<PlayerData> playerData = Sierra.getPlugin()
            .getSierraDataManager()
            .getPlayerData(PacketEvents.getAPI().getPlayerManager().getUser(player));

        if (playerData == null || playerData.get() == null) {
            player.sendMessage(Sierra.PREFIX + " §cNo data found");
            return;
        }
        printMonitor(Objects.requireNonNull(playerData.get()), player);
    }


    /**
     * Prints the monitor information related to the player.
     *
     * @param playerData the PlayerData object representing the player's data
     * @param player     the Player object representing the player
     */
    private void printMonitor(PlayerData playerData, Player player) {

        TimingHandler timingProcessor = playerData.getTimingProcessor();
        player.sendMessage(Sierra.PREFIX + " §c§lPackets:");
        sendTiming(timingProcessor.getPacketReceiveTask(), "Ingoing Packets", player);
        sendTiming(timingProcessor.getPacketSendTask(), "Outgoing Packets", player);
        player.sendMessage(Sierra.PREFIX + " §c§lEnvironment:");
        sendTiming(timingProcessor.getMovementTask(), "Movement Task", player);
    }

    /**
     * Sends a timing message to the player.
     *
     * @param packetReceiveTiming the timing object representing the packet receive timing
     * @param title               the title of the timing message
     * @param player              the player to send the timing message to
     */
    private void sendTiming(Timing packetReceiveTiming, String title, Player player) {
        player.sendMessage(String.format("%s  §8- §f%s §7(%.5fms)", Sierra.PREFIX, title, packetReceiveTiming.delay()));
    }

    /**
     * Generates a list of strings based on the given ID and arguments.
     *
     * @param id   the ID used to generate the list of strings
     * @param args an array of strings representing the arguments
     * @return a list of strings generated based on the ID and arguments
     */
    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("monitor");
        }
        return Collections.emptyList();
    }

    /**
     * Returns the description of the method or command.
     *
     * @return the description of the method or command as a String
     */
    @Override
    public String description() {
        return "Performance analysis";
    }
}
