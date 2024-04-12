package de.feelix.sierra.command;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.command.api.BukkitAbstractCommand;
import de.feelix.sierra.command.api.SierraArguments;
import de.feelix.sierra.command.api.SierraLabel;
import de.feelix.sierra.command.api.SierraSender;
import de.feelix.sierra.command.impl.*;
import de.feelix.sierraapi.commands.ISierraCommand;
import de.feelix.sierraapi.events.AsyncSierraCommandEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SierraCommand implements CommandExecutor, TabExecutor {

    /**
     * The commands variable is a HashMap that stores instances of objects implementing the ISierraCommand interface.
     * The key of the HashMap is a String representing the name of the command, and the value is the corresponding ISierraCommand object.
     */
    private final HashMap<String, ISierraCommand> commands = new HashMap<>();

    /**
     * The SierraCommand class represents a command that can be executed in-game.
     * It initializes various sub-commands such as AlertsCommand, VersionCommand, ReloadCommand, and HistoryCommand.
     * These sub-commands are registered with Bukkit's command manager so that they can be executed when the corresponding command is entered in-game.
     */
    public SierraCommand() {
        this.commands.put("alerts", new AlertsCommand());
        this.commands.put("version", new VersionCommand());
        this.commands.put("reload", new ReloadCommand());
        this.commands.put("history", new HistoryCommand());
    }

    /**
     * Executes the specified command when it is called. This method is called when a player or the console executes the command.
     *
     * @param sender the CommandSender who issued the command
     * @param command the Command being executed
     * @param label the alias of the command used
     * @param args an array of arguments passed with the command
     * @return true if the command was executed successfully, false otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {

        AsyncSierraCommandEvent event = new AsyncSierraCommandEvent(command.getName(), label);

        Bukkit.getScheduler()
            .runTaskAsynchronously(Sierra.getPlugin(), () -> Bukkit.getPluginManager().callEvent(event));

        //
        if (!sender.hasPermission("sierra.command")) {
            CommandHelper.sendVersionOutput(new SierraSender(sender));
            return true;
        }

        CompletableFuture.runAsync(() -> {
            if (args.length > 0) {

                String firstInput = args[0];

                if (!sender.hasPermission("sierra.command")) {
                    sendMainCommandSyntax(sender);
                    return;
                }

                if (!commands.containsKey(firstInput)) {
                    sendMainCommandSyntax(sender);
                    return;
                }

                if (commands.get(firstInput) != null) {

                    ISierraCommand iSierraCommand = commands.get(firstInput);

                    iSierraCommand.process(new SierraSender(sender), new BukkitAbstractCommand(command),
                                           new SierraLabel(label), new SierraArguments(args)
                    );
                }
            } else {
                sendMainCommandSyntax(sender);
            }
        });
        return true;
    }

    /**
     * Sends the main command syntax to the given command sender.
     *
     * @param commandSender the CommandSender to send the syntax to
     */
    private void sendMainCommandSyntax(CommandSender commandSender) {
        String prefix = Sierra.PREFIX;
        commandSender.sendMessage(prefix + " §fSubcommands §7(/sierra)");
        commands.forEach((s, iSierraCommand) -> {
            if (commandSender.hasPermission("sierra.command")) {
                String description = iSierraCommand.description();
                String format      = "%s §c%s§7: §f%s";
                commandSender.sendMessage(String.format(format, prefix, s, description));
            }
        });
    }

    /**
     * Generates tab completions for the "sierra" command.
     *
     * @param sender the sender of the command
     * @param command the command being executed
     * @param alias the command alias used
     * @param args the arguments passed to the command
     * @return a list of tab completions for the command, or null if no completions are available
     */
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @Nullable String alias, String[] args) {

        if (!command.getName().equalsIgnoreCase("sierra")) {
            return null;
        }

        String defaultBukkitPermission = "sierra.command";

        if (!sender.hasPermission(defaultBukkitPermission)) {
            return null;
        }

        List<String> keys = new ArrayList<>();

        commands.forEach((s, iSierraCommand) -> keys.addAll(iSierraCommand.fromId(args.length, args)));

        if (keys.isEmpty()) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        }
        return keys;
    }
}
