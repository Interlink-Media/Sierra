package de.feelix.sierraapi.commands;

import org.bukkit.command.Command;

/**
 * IBukkitAbstractCommand represents an interface for a wrapper around a Bukkit Command.
 * An instance of IBukkitAbstractCommand can be obtained by implementing this interface.
 * It provides a single method, getBukkitCommand(), which returns the wrapped Bukkit Command object.
 */
public interface IBukkitAbstractCommand {

    /**
     * Returns the wrapped Bukkit Command object.
     *
     * @return the wrapped Bukkit Command object
     */
    Command getBukkitCommand();
}
