package de.feelix.sierra.command;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.command.api.BukkitAbstractCommand;
import de.feelix.sierra.command.api.SierraArguments;
import de.feelix.sierra.command.api.SierraLabel;
import de.feelix.sierra.command.impl.*;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.annotation.NotNull;
import de.feelix.sierraapi.annotation.Nullable;
import de.feelix.sierraapi.commands.ISierraCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SierraCommand implements CommandExecutor, TabExecutor {

    private static final HashMap<String, ISierraCommand> commands       = new HashMap<>();
    private static final String                          MESSAGE_FORMAT = "%s {offset-color}%s§7: §f%s";
    private static final String                          MESSAGE_PREFIX = Sierra.PREFIX + " §fSubcommands §7(/sierra)";

    public SierraCommand() {
        commands.put("reload", new ReloadCommand());
        commands.put("alerts", new AlertsCommand());
        commands.put("mitigation", new MitigationCommand());
        commands.put("info", new InfoCommand());
        commands.put("version", new VersionCommand());
        commands.put("monitor", new MonitorCommand());
        commands.put("history", new HistoryCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {

        if (!(sender instanceof Player)) return false;

        Player     player     = (Player) sender;
        User       user       = getPlayerUserDetails(player);
        PlayerData playerData = getPlayerDataFromUser(user);

        if (hasNoPermission(sender)) {
            sendVersionOutputToUser(user);
            return true;
        }

        CompletableFuture.runAsync(() -> handleCommandAsync(sender, command, label, args, user, playerData));

        return true;
    }

    private User getPlayerUserDetails(Player player) {
        return PacketEvents.getAPI().getPlayerManager().getUser(player);
    }

    private PlayerData getPlayerDataFromUser(User user) {
        return Sierra.getPlugin().getSierraDataManager().getPlayerData(user).get();
    }

    private void sendVersionOutputToUser(User user) {
        CommandHelper.sendVersionOutput(user);
    }

    private void handleCommandAsync(CommandSender sender, Command command, String label, String[] args, User user,
                                    PlayerData playerData) {
        if (args.length > 0) {
            handleWithArg(sender, command, label, args, user, playerData);
        } else {
            sendMainCommandSyntax(sender);
        }
    }

    private void handleWithArg(CommandSender sender, Command command, String label, String[] args, User user,
                               PlayerData playerData) {
        String         firstInput     = args[0];
        ISierraCommand iSierraCommand = commands.get(firstInput);

        if (iSierraCommand != null) {
            processSierraCommand(sender, user, playerData, command, label, args, iSierraCommand);
        } else {
            sendMainCommandSyntax(sender);
        }
    }

    private void processSierraCommand(CommandSender sender, User user, PlayerData playerData, Command command,
                                      String label, String[] args, ISierraCommand iSierraCommand) {

        if (iSierraCommand.permission() != null && !sender.hasPermission(iSierraCommand.permission())) {
            CommandHelper.sendVersionOutput(user);
            return;
        }
        iSierraCommand.process(user, playerData, new BukkitAbstractCommand(command),
                               new SierraLabel(label), new SierraArguments(args)
        );
    }

    private void sendMainCommandSyntax(CommandSender commandSender) {
        commandSender.sendMessage(MESSAGE_PREFIX);

        final HashMap<String, ISierraCommand> sierraCommandList = new HashMap<>();

        commands.forEach((s, iSierraCommand) -> {
            if (commandSender.hasPermission("sierra.command")) {
                if (iSierraCommand.permission() == null) {
                    sierraCommandList.put(s, iSierraCommand);
                } else if (commandSender.hasPermission(iSierraCommand.permission())) {
                    sierraCommandList.put(s, iSierraCommand);
                }
            }
        });

        sierraCommandList.forEach((s, iSierraCommand) -> {
            String description = iSierraCommand.description();

            commandSender.sendMessage(String.format(MESSAGE_FORMAT.replace("{offset-color}", Sierra.getPlugin()
                .getSierraConfigEngine()
                .messages()
                .getString("layout.offset-color", "§b")), Sierra.PREFIX, s, description));
        });
        String format = "%s §7Use <tab> so see %s completions";
        commandSender.sendMessage(String.format(format, Sierra.PREFIX, FormatUtils.numberToText(
            sierraCommandList.size())));
    }

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

    private boolean isValidCommand(@NotNull Command command) {
        return command.getName().equalsIgnoreCase("sierra");
    }

    private boolean hasNoPermission(@NotNull CommandSender sender) {
        return !sender.hasPermission("sierra.command");
    }

    private List<String> getGeneratedKeys(CommandSender commandSender, String[] args) {
        List<String> keys = new ArrayList<>();
        commands.forEach((s, iSierraCommand) -> keys.addAll(
            this.getKeysForCommand(iSierraCommand, commandSender, args.length, args)));
        return keys;
    }

    private List<String> getKeysForCommand(ISierraCommand iSierraCommand, CommandSender commandSender, int length,
                                           String[] args) {
        // Command is eligible for processing if no permissions required or if permissions are granted.
        boolean isEligible =
            iSierraCommand.permission() == null || commandSender.hasPermission(iSierraCommand.permission());
        return isEligible ? iSierraCommand.fromId(length, args) : Collections.emptyList();
    }

    private List<String> getOnlinePlayerNames() {
        List<String> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            list.add(name);
        }
        return list;
    }
}
