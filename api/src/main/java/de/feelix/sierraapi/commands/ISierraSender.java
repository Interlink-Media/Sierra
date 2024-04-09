package de.feelix.sierraapi.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface ISierraSender {

    CommandSender getSender();

    Player getSenderAsPlayer();
}
