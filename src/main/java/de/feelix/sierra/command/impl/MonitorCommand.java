package de.feelix.sierra.command.impl;

import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.message.ConfigValue;
import de.feelix.sierraapi.commands.*;
import de.feelix.sierraapi.timing.Timing;
import de.feelix.sierraapi.timing.TimingHandler;
import de.feelix.sierraapi.user.impl.SierraUser;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

/**
 * MonitorCommand is a class that represents a command that prints monitoring information related to the player.
 * It implements the ISierraCommand interface, which defines the necessary methods that need to be implemented by a
 * command class.
 */
public class MonitorCommand implements ISierraCommand {

    /**
     * The process method is responsible for processing the command and printing the performance monitor information
     * related to the player.
     *
     * @param user            the User associated with the command execution
     * @param sierraUser      the SierraUser representing the user in the Sierra API
     * @param abstractCommand the IBukkitAbstractCommand object representing the wrapped Bukkit Command
     * @param sierraLabel     the ISierraLabel object representing the label of the initial symbol
     * @param sierraArguments the ISierraArguments object representing the arguments passed with the command
     */
    @Override
    public void process(User user, SierraUser sierraUser, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel,
                        ISierraArguments sierraArguments) {

        user.sendMessage(new ConfigValue(
            "commands.monitor.header",
            "{prefix} &fPerformance monitor &7(Your data)",
            true
        ).replacePrefix().colorize().getMessageValue());

        WeakReference<PlayerData> playerData = Sierra.getPlugin().getSierraDataManager().getPlayerData(user);

        if (playerData == null || playerData.get() == null) {
            user.sendMessage(
                new ConfigValue(
                    "commands.monitor.nothing-found",
                    "{prefix} &cNo data found!",
                    true
                ).replacePrefix().colorize().getMessageValue());
            return;
        }
        printMonitor(user, sierraUser);
    }

    /**
     * Prints performance monitor information related to the player.
     *
     * @param user       the User associated with the command execution
     * @param sierraUser the SierraUser representing the user in the Sierra API
     */
    private void printMonitor(User user, SierraUser sierraUser) {

        TimingHandler timingProcessor = sierraUser.timingHandler();
        user.sendMessage(
            new ConfigValue(
                "commands.monitor.packets-header",
                "{prefix} &b&lPackets:",
                true
            ).replacePrefix().colorize().getMessageValue());
        sendTiming(timingProcessor.getPacketReceiveTask(), "Ingoing Packets", user);
        sendTiming(timingProcessor.getPacketSendTask(), "Outgoing Packets", user);
        user.sendMessage(
            new ConfigValue(
                "commands.monitor.environment-header",
                "{prefix} &b&lEnvironment:",
                true
            ).replacePrefix().colorize().getMessageValue());
        sendTiming(timingProcessor.getMovementTask(), "Movement Task", user);
    }

    /**
     * Sends a timing message to the user.
     *
     * @param packetReceiveTiming the Timing object representing the timing of the received packet
     * @param title               the title of the timing message
     * @param user                the User object to send the message to
     */
    private void sendTiming(Timing packetReceiveTiming, String title, User user) {
        user.sendMessage(
            new ConfigValue(
                "commands.monitor.entry",
                "{prefix}  &8- &f{title} &7({delay}ms)",
                true
            ).replacePrefix()
                .replace("{title}", title)
                .replace("{delay}", String.format("%.5f", packetReceiveTiming.delay()))
                .colorize().getMessageValue());
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

    @Override
    public String permission() {
        return "sierra.command.monitor";
    }
}
