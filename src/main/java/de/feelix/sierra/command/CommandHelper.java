package de.feelix.sierra.command;

import de.feelix.sierra.Sierra;
import de.feelix.sierraapi.commands.ISierraSender;
import org.bukkit.command.CommandSender;

/**
 * The CommandHelper class provides helper methods for processing and sending commands related to the Sierra plugin.
 */
public class CommandHelper {

    /**
     * Sends the version output of the Sierra plugin to the specified {@link ISierraSender}.
     * Includes information about the plugin version and a link to the Discord server.
     *
     * @param sierraSender the ISierraSender object representing the sender of the command.
     */
    public static void sendVersionOutput(ISierraSender sierraSender) {
        final String  sierraPrefix  = Sierra.PREFIX;
        Sierra        sierraPlugin  = Sierra.getPlugin();
        CommandSender commandSender = sierraSender.getSender();

        String pluginVersionMessage = createVersionMessage(sierraPlugin, commandSender);

        commandSender.sendMessage(sierraPrefix + " §fRunning §cSierra " + pluginVersionMessage);
        commandSender.sendMessage(sierraPrefix + " §fMore info at §cdiscord.gg/squarecode");
    }

    /**
     * Creates a version message for the Sierra plugin based on the given plugin instance and command sender.
     *
     * @param sierraPlugin    the instance of the Sierra plugin
     * @param commandSender   the command sender
     * @return the version message
     */
    private static String createVersionMessage(Sierra sierraPlugin, CommandSender commandSender) {
        String pluginVersion = "§7(" + sierraPlugin.getDescription().getVersion() + ")";
        boolean isVersionHidden = sierraPlugin.getSierraConfigEngine().config().getBoolean(
            "hideVersion",
            true
        );
        boolean isPermissionGranted = commandSender.hasPermission("sierra.command");

        if (isVersionHidden && !isPermissionGranted) {
            pluginVersion = "§7(version hidden)";
        }
        return pluginVersion;
    }
}
