package de.feelix.sierra.command;

import de.feelix.sierra.Sierra;
import de.feelix.sierraapi.commands.ISierraSender;
import org.bukkit.command.CommandSender;

public class CommandHelper {

    /**
     * Sends the version output to the given sender.
     * The version output includes the Sierra plugin version and additional information.
     * If the plugin version is hidden in the configuration and the sender does not have the appropriate permission,
     * the version will be replaced with a message indicating that it is hidden.
     *
     * @param sierraSender the ISierraSender object representing the sender
     * @since Unknown
     */
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
