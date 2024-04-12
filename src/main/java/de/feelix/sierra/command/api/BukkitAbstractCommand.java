package de.feelix.sierra.command.api;

import de.feelix.sierraapi.commands.IBukkitAbstractCommand;
import org.bukkit.command.Command;

/**
 * The BukkitAbstractCommand class represents a wrapper around a Bukkit Command.
 * It implements the IBukkitAbstractCommand interface.
 */
public class BukkitAbstractCommand implements IBukkitAbstractCommand {

    /**
     * The private final variable "command" represents a Bukkit Command object.
     * It is of type Command and is used to wrap a Bukkit command within a BukkitAbstractCommand object.
     * This variable is used by the containing class BukkitAbstractCommand to interact with the wrapped Bukkit command.
     *
     * @see BukkitAbstractCommand
     * @see IBukkitAbstractCommand
     */
    private final Command command;

    /**
     * Constructs a new BukkitAbstractCommand object.
     *
     * @param command the Bukkit Command to be wrapped
     *
     * @throws IllegalArgumentException if the command is null
     */
    public BukkitAbstractCommand(Command command) {
        this.command = command;
    }

    /**
     * Returns the Bukkit Command object associated with this BukkitAbstractCommand.
     *
     * @return the Bukkit Command object
     */
    @Override
    public Command getBukkitCommand() {
        return command;
    }
}
