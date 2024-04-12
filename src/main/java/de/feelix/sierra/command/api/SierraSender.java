package de.feelix.sierra.command.api;

import de.feelix.sierraapi.commands.ISierraSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * The SierraSender class represents a wrapper around a CommandSender object that provides additional functionality for sending commands and messages to the sender.
 */
public class SierraSender implements ISierraSender {

    /**
     * The sender variable represents the CommandSender object that is associated with this instance.
     * It is a private final instance variable in the SierraSender class.
     * <p>
     * The CommandSender interface is implemented in the SierraSender class and provides access to various methods for sending commands and messages to the sender.
     * <p>
     * Example usage:
     * <pre>
     *     // Initializing a new SierraSender object with a CommandSender
     *     CommandSender commandSender = ...;
     *     SierraSender sierraSender = new SierraSender(commandSender);
     *
     *     // Accessing the sender CommandSender object
     *     CommandSender sender = sierraSender.getSender();
     *
     *     // Accessing the sender as a Player, if applicable
     *     Player player = sierraSender.getSenderAsPlayer();
     *     if (player != null) {
     *         // Perform operations specific to a Player
     *     }
     * </pre>
     */
    private final CommandSender sender;

    /**
     * Constructs a new SierraSender object with the given CommandSender.
     * The SierraSender class represents a wrapper around a CommandSender object that provides additional functionality for sending commands and messages to the sender.
     *
     * @param sender the CommandSender object to wrap
     */
    public SierraSender(CommandSender sender) {
        this.sender = sender;
    }

    /**
     * Retrieves the CommandSender associated with this SierraSender instance.
     * The CommandSender interface provides methods for sending commands and messages to the sender.
     *
     * @return the CommandSender object associated with this SierraSender instance
     */
    @Override
    public CommandSender getSender() {
        return sender;
    }

    /**
     * Retrieves the sender as a Player, if applicable.
     *
     * @return the sender as a Player, or null if the sender is not an instance of Player
     */
    @Override
    public Player getSenderAsPlayer() {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }
}
