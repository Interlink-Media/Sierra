package de.feelix.sierraapi.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * The ISierraSender interface represents a wrapper around a CommandSender object that provides additional functionality for sending commands and messages to the sender.
 */
public interface ISierraSender {

    /**
     * Retrieves the sender of the command.
     *
     * @return The CommandSender object representing the sender of the command.
     */
    CommandSender getSender();

    /**
     * Retrieves the sender of the command as a Player.
     *
     * @return The Player object representing the sender of the command.
     */
    Player getSenderAsPlayer();
}
