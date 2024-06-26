package de.feelix.sierra.command;

import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * The CommandHelper class provides helper methods for processing and sending commands related to the Sierra plugin.
 */
public class CommandHelper {

    /**
     * Sends the version output to the command sender.
     *
     * @param user the user containing the command sender
     */
    public static void sendVersionOutput(User user) {
        final String sierraPrefix = Sierra.PREFIX;
        Sierra       sierraPlugin = Sierra.getPlugin();

        String pluginVersionMessage = createVersionMessage(sierraPlugin, user);

        user.sendMessage(sierraPrefix + " §fRunning " + Sierra.getPlugin()
            .getSierraConfigEngine()
            .messages()
            .getString("layout.offset-color", "§b") + "Sierra " + pluginVersionMessage);
        user.sendMessage(sierraPrefix + " §fMore info at {offset-color}discord.gg/squarecode".replace(
            "{offset-color}",
            Sierra.getPlugin().getSierraConfigEngine().messages().getString("layout.offset-color", "§b")
        ));
    }

    /**
     * Creates a version message based on the Sierra plugin and user's permission.
     *
     * @param sierraPlugin the Sierra plugin instance
     * @param user the user object containing the player information
     * @return the version message
     */
    private static String createVersionMessage(Sierra sierraPlugin, User user) {
        String pluginVersion = "§7(" + sierraPlugin.getDescription().getVersion() + ")";
        boolean isVersionHidden = sierraPlugin.getSierraConfigEngine().config().getBoolean(
            "hide-version",
            true
        );

        Player player = Bukkit.getPlayer(user.getUUID());

        boolean isPermissionGranted = player != null && player.hasPermission("sierra.command");

        if (isVersionHidden && !isPermissionGranted) {
            pluginVersion = "§7(version hidden)";
        }
        return pluginVersion;
    }
}
