package de.feelix.sierra.command;

import de.feelix.sierra.Sierra;
import de.feelix.sierraapi.commands.ISierraSender;
import org.bukkit.command.CommandSender;

public class CommandHelper {

    public static void sendVersionOutput(ISierraSender sierraSender) {
        final String  prefix = Sierra.PREFIX;
        Sierra        plugin = Sierra.getPlugin();
        CommandSender sender = sierraSender.getSender();

        String  pluginVersion = "§7(" + plugin.getDescription().getVersion() + ")";
        boolean hideVersion   = plugin.getSierraConfigEngine().config().getBoolean("hideVersion", true);
        boolean hasPermission = sender.hasPermission("sierra.command");

        if (hideVersion && !hasPermission) {
            pluginVersion = "§7(version hidden)";
        }

        sender.sendMessage(prefix + " §fRunning §cSierra " + pluginVersion);
        sender.sendMessage(prefix + " §fMore info at §cdiscord.gg/squarecode");
    }
}
