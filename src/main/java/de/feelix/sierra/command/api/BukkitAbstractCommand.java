package de.feelix.sierra.command.api;

import de.feelix.sierraapi.commands.IBukkitAbstractCommand;
import org.bukkit.command.Command;

public class BukkitAbstractCommand implements IBukkitAbstractCommand {

    private final Command command;

    public BukkitAbstractCommand(Command command) {
        this.command = command;
    }

    @Override
    public Command getBukkitCommand() {
        return command;
    }
}
