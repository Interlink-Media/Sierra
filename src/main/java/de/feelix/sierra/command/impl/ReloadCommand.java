package de.feelix.sierra.command.impl;

import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.utilities.message.ConfigValue;
import de.feelix.sierraapi.commands.*;
import de.feelix.sierraapi.user.impl.SierraUser;

import java.util.Collections;
import java.util.List;

/**
 * The ReloadCommand class implements the ISierraCommand interface and represents a command
 * that can be used to reload the configuration of the Sierra plugin.
 */
public class ReloadCommand implements ISierraCommand {

    /**
     * Processes the command to reload the configuration.
     * This method removes all cached config files, resets the prefix, and sets up the SierraDiscordGateway.
     * It also sends a message to the sender indicating that the configuration has been reloaded successfully.
     *
     * @param user            the user associated with the command
     * @param sierraUser      the Sierra user associated with the command
     * @param abstractCommand the abstract command associated with the command
     * @param sierraLabel     the Sierra label associated with the command
     * @param sierraArguments the Sierra arguments associated with the command
     */
    @Override
    public void process(User user, SierraUser sierraUser, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        // Remove all cached config files
        Sierra.getPlugin().getSierraConfigEngine().invalidateCache();

        // Reset prefix
        Sierra.getPlugin().setPrefix();

        Sierra.getPlugin().getSierraDiscordGateway().setup();

        user.sendMessage(new ConfigValue("commands.reload.success",
                                         "{prefix} &fConfiguration reloaded &asuccessfully",
                                         true
        ).replacePrefix().colorize().getMessage());
    }

    /**
     * Converts an id to a list of strings based on the given id and arguments.
     *
     * @param id   the id to convert
     * @param args the arguments to use during conversion
     * @return a list of strings based on the id and arguments
     */
    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }

    /**
     * Retrieves the description of the method which is to reload the configuration.
     *
     * @return The description of the method to reload the configuration.
     */
    @Override
    public String description() {
        return "Reloads the config";
    }

    @Override
    public String permission() {
        return "sierra.command.reload";
    }
}
