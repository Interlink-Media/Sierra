package de.feelix.sierra.command;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.command.api.BukkitAbstractCommand;
import de.feelix.sierra.command.api.SierraArguments;
import de.feelix.sierra.command.api.SierraLabel;
import de.feelix.sierra.command.api.SierraSender;
import de.feelix.sierra.command.impl.*;
import de.feelix.sierraapi.commands.ISierraCommand;
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

/**
 * SierraCommand represents a command that can be executed in-game.
 * It initializes various sub-commands such as AlertsCommand, VersionCommand, ReloadCommand, and HistoryCommand.
 * These sub-commands are registered with Bukkit's command manager so that they can be executed when the
 * corresponding command is entered in-game.
 */
public class SierraCommand implements CommandExecutor, TabExecutor {

    /**
     * The commands variable is a HashMap that stores instances of objects implementing the ISierraCommand interface.
     * The key of the HashMap is a String representing the name of the command, and the value is the corresponding
     * ISierraCommand object.
     */
    private final HashMap<String, ISierraCommand> commands = new HashMap<>();

    /**
     * The MESSAGE_FORMAT variable is a private static final field representing the format of a message.
     * It is a string format that includes placeholders for the message sender, message type, and message content.
     * <p>
     * The format is as follows:
     * "{sender} §c{type}§7: §f{content}"
     * <p>
     * This format can be used to create formatted messages by replacing the placeholders with actual values.
     * <p>
     * Example usage:
     * String sender = "John";
     * String type = "Error";
     * String content = "An error occurred.";
     * String message = String.format(MESSAGE_FORMAT, sender, type, content);
     * <p>
     * The message will be: "John §cError§7: §fAn error occurred."
     */
    private static final String MESSAGE_FORMAT = "%s §c%s§7: §f%s";

    /**
     * The MESSAGE_PREFIX variable represents the prefix for messages sent by the Sierra plugin.
     * It is a static, private field and is obtained by concatenating the Sierra.PREFIX constant with the "
     * §fSubcommands §7(/sierra)" string.
     * The prefix is translated using the '&' character as a color code indicator.
     */
    private static final String MESSAGE_PREFIX = Sierra.PREFIX + " §fSubcommands §7(/sierra)";

    /**
     * The SierraCommand class represents a command that can be executed in-game.
     * It initializes various sub-commands such as AlertsCommand, VersionCommand, ReloadCommand, and HistoryCommand.
     * These sub-commands are registered with Bukkit's command manager so that they can be executed when the
     * corresponding command is entered in-game.
     */
    public SierraCommand() {
        this.commands.put("reload", new ReloadCommand());
        this.commands.put("alerts", new AlertsCommand());
        this.commands.put("version", new VersionCommand());
        this.commands.put("monitor", new MonitorCommand());
        this.commands.put("history", new HistoryCommand());
    }

    /**
     * Executes the Sierra command.
     *
     * @param sender  the command sender
     * @param command the command being executed
     * @param label   the alias of the command used
     * @param args    the arguments provided for the command
     * @return true if the command was executed successfully, false otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {

        SierraSender sierraSender = new SierraSender(sender);

        if (hasNoPermission(sender)) {
            CommandHelper.sendVersionOutput(sierraSender);
            return true;
        }

        CompletableFuture.runAsync(() -> {
            if (args.length > 0) {
                String         firstInput     = args[0];
                ISierraCommand iSierraCommand = commands.get(firstInput);
                if (iSierraCommand != null) {

                    if (iSierraCommand.permission() != null && !sender.hasPermission(iSierraCommand.permission())) {
                        CommandHelper.sendVersionOutput(sierraSender);
                        return;
                    }

                    iSierraCommand.process(sierraSender, new BukkitAbstractCommand(command),
                                           new SierraLabel(label), new SierraArguments(args)
                    );
                } else {
                    sendMainCommandSyntax(sender);
                }
            } else {
                sendMainCommandSyntax(sender);
            }
        });
        return true;
    }

    /**
     * Sends the main command syntax to the specified command sender.
     *
     * @param commandSender the command sender to send the syntax to
     */
    private void sendMainCommandSyntax(CommandSender commandSender) {
        commandSender.sendMessage(MESSAGE_PREFIX);
        commands.forEach((s, iSierraCommand) -> {
            if (commandSender.hasPermission("sierra.command")) {
                String description = iSierraCommand.description();

                if (iSierraCommand.permission() == null) {
                    commandSender.sendMessage(String.format(MESSAGE_FORMAT, Sierra.PREFIX, s, description));
                } else if (commandSender.hasPermission(iSierraCommand.permission())) {
                    commandSender.sendMessage(String.format(MESSAGE_FORMAT, Sierra.PREFIX, s, description));
                }
            }
        });
    }

    /**
     * This method is called when tab-completion for this command is requested.
     * It provides a list of valid completions for the current command arguments.
     *
     * @param sender  the command sender
     * @param command the command being completed
     * @param alias   the alias used for the command
     * @param args    the arguments provided for the command
     * @return a list of valid completions for the current command arguments
     */
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @Nullable String alias, String[] args) {
        if (!isValidCommand(command)) {
            return null;
        }
        if (hasNoPermission(sender)) {
            return null;
        }
        List<String> keys = getGeneratedKeys(sender, args);
        return keys.isEmpty() ? getOnlinePlayerNames() : keys;
    }

    /**
     * Checks if the given command is a valid command.
     *
     * @param command the command to validate
     * @return true if the command is valid, false otherwise
     */
    private boolean isValidCommand(@NotNull Command command) {
        return command.getName().equalsIgnoreCase("sierra");
    }

    /**
     * Checks if the specified CommandSender has permission to execute a command.
     *
     * @param sender the CommandSender to check permission for
     * @return true if the sender has permission, false otherwise
     */
    private boolean hasNoPermission(@NotNull CommandSender sender) {
        return !sender.hasPermission("sierra.command");
    }


    private List<String> getGeneratedKeys(CommandSender commandSender, String[] args) {
        List<String> keys = new ArrayList<>();
        commands.forEach((s, iSierraCommand) -> {
            if (iSierraCommand.permission() == null) {
                keys.addAll(iSierraCommand.fromId(args.length, args));
            } else if (commandSender.hasPermission(iSierraCommand.permission())) {
                keys.addAll(iSierraCommand.fromId(args.length, args));
            }
        });
        return keys;
    }

    /**
     * Retrieves a list of names of all players who are currently online.
     *
     * @return a List of String containing the names of all online players.
     */
    private List<String> getOnlinePlayerNames() {
        List<String> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            list.add(name);
        }
        return list;
    }
}
