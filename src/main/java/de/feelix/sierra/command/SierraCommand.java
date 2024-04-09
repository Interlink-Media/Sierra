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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SierraCommand implements CommandExecutor, TabExecutor {

    private final HashMap<String, ISierraCommand> commands = new HashMap<>();

    public SierraCommand() {
        this.commands.put("alerts", new AlertsCommand());
        this.commands.put("version", new VersionCommand());
        this.commands.put("reload", new ReloadCommand());
        this.commands.put("history", new HistoryCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String[] args) {

        AsyncSierraCommandEvent event = new AsyncSierraCommandEvent(command.getName(), label);

        Bukkit.getScheduler()
            .runTaskAsynchronously(Sierra.getPlugin(), () -> Bukkit.getPluginManager().callEvent(event));

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

    @Nullable
    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command command,
                                      @Nullable String alias, String[] args) {

        String defaultBukkitPermission = "sierra.command";

        if (!sender.hasPermission(defaultBukkitPermission)) {
            return Collections.emptyList();
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
