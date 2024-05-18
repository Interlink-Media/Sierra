package de.feelix.sierra.command.impl;


import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.command.CommandHelper;
import de.feelix.sierraapi.commands.*;
import de.feelix.sierraapi.user.impl.SierraUser;

import java.util.Collections;
import java.util.List;

/**
 * VersionCommand is a class that implements the ISierraCommand interface.
 * It represents a command that displays the version information of the Sierra plugin.
 */
public class VersionCommand implements ISierraCommand {

    /**
     * This method processes the given inputs.
     *
     * @param user            the user initiating the process
     * @param sierraUser      the Sierra user associated with the process
     * @param abstractCommand the abstract command object used for processing
     * @param sierraLabel     the Sierra label used for the process
     * @param sierraArguments the arguments provided for the process
     */
    @Override
    public void process(User user, SierraUser sierraUser, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        CommandHelper.sendVersionOutput(user);
    }

    /**
     * Converts an ID to a list of strings based on the given ID and arguments.
     *
     * @param id   the ID to convert
     * @param args the arguments
     * @return a list of strings based on the ID and arguments
     */
    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("version");
        }
        return Collections.emptyList();
    }

    /**
     * Retrieves the description of the method.
     *
     * @return The description of the method
     */
    @Override
    public String description() {
        return "View version info";
    }

    @Override
    public String permission() {
        return null;
    }
}
