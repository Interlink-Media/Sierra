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

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SierraCommand implements CommandExecutor, TabExecutor {

    private static final Map<String, ISierraCommand> COMMANDS = new HashMap<>();
    private static final String MESSAGE_FORMAT = "%s {offset-color}%s§7: §f%s";
    private static final String MESSAGE_PREFIX = Sierra.PREFIX + " §fSubcommands §7(/sierra)";

    static {
        COMMANDS.put("reload", new ReloadCommand());
        COMMANDS.put("alerts", new AlertsCommand());
        COMMANDS.put("mitigation", new MitigationCommand());
        COMMANDS.put("info", new InfoCommand());
        COMMANDS.put("version", new VersionCommand());
        COMMANDS.put("monitor", new MonitorCommand());
        COMMANDS.put("history", new HistoryCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        PlayerData playerData = Sierra.getPlugin().getSierraDataManager().getPlayerData(user).get();

        if (playerData == null || !sender.hasPermission("sierra.command")) {
            sendVersionOutputToUser(user);
            return true;
        }

        CompletableFuture.runAsync(() -> handleCommandAsync(sender, command, label, args, user, playerData));
        return true;
    }

    private void sendVersionOutputToUser(User user) {
        CommandHelper.sendVersionOutput(user);
    }

    private void handleCommandAsync(CommandSender sender, Command command, String label, String[] args, User user, PlayerData playerData) {
        if (args.length > 0) {
            handleWithArg(sender, command, label, args, user, playerData);
        } else {
            sendMainCommandSyntax(sender);
        }
    }

    private void handleWithArg(CommandSender sender, Command command, String label, String[] args, User user, PlayerData playerData) {
        ISierraCommand iSierraCommand = COMMANDS.get(args[0]);

        if (iSierraCommand != null) {
            processSierraCommand(sender, user, playerData, command, label, args, iSierraCommand);
        } else {
            sendMainCommandSyntax(sender);
        }
    }

    private void processSierraCommand(CommandSender sender, User user, PlayerData playerData, Command command, String label, String[] args, ISierraCommand iSierraCommand) {
        if (iSierraCommand.permission() != null && !sender.hasPermission(iSierraCommand.permission())) {
            CommandHelper.sendVersionOutput(user);
            return;
        }
        iSierraCommand.process(user, playerData, new BukkitAbstractCommand(command), new SierraLabel(label), new SierraArguments(args));
    }

    private void sendMainCommandSyntax(CommandSender commandSender) {
        commandSender.sendMessage(MESSAGE_PREFIX);
        Map<String, ISierraCommand> sierraCommandList = new HashMap<>();

        COMMANDS.forEach((s, iSierraCommand) -> {
            if (commandSender.hasPermission("sierra.command") &&
                (iSierraCommand.permission() == null || commandSender.hasPermission(iSierraCommand.permission()))) {
                sierraCommandList.put(s, iSierraCommand);
            }
        });

        sierraCommandList.forEach((s, iSierraCommand) -> {
            String description = iSierraCommand.description();
            commandSender.sendMessage(String.format(MESSAGE_FORMAT.replace("{offset-color}", Sierra.getPlugin().getSierraConfigEngine().messages().getString("layout.offset-color", "§b")), Sierra.PREFIX, s, description));
        });

        commandSender.sendMessage(String.format("%s §7Use <tab> so see %s completions", Sierra.PREFIX, FormatUtils.numberToText(sierraCommandList.size())));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @Nullable String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("sierra") || !sender.hasPermission("sierra.command")) {
            return null;
        }
        return getGeneratedKeys(sender, args);
    }

    private List<String> getGeneratedKeys(CommandSender commandSender, String[] args) {
        List<String> keys = new ArrayList<>();
        COMMANDS.forEach((s, iSierraCommand) -> keys.addAll(getKeysForCommand(iSierraCommand, commandSender, args.length, args)));
        return keys.isEmpty() ? getOnlinePlayerNames() : keys;
    }

    private List<String> getKeysForCommand(ISierraCommand iSierraCommand, CommandSender commandSender, int length, String[] args) {
        boolean isEligible = iSierraCommand.permission() == null || commandSender.hasPermission(iSierraCommand.permission());
        return isEligible ? iSierraCommand.fromId(length, args) : Collections.emptyList();
    }

    private List<String> getOnlinePlayerNames() {
        List<String> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            list.add(player.getName());
        }
        return list;
    }
}
