package de.feelix.sierra.command.api;

import de.feelix.sierraapi.commands.ISierraSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SierraSender implements ISierraSender {

    private final CommandSender sender;

    public SierraSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public CommandSender getSender() {
        return sender;
    }

    @Override
    public Player getSenderAsPlayer() {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }
}
