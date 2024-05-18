package de.feelix.sierraapi.commands;

import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierraapi.user.impl.SierraUser;

import java.util.List;

/**
 * ISierraCommand is an interface that represents a command in the Sierra command framework.
 * It defines the necessary methods that need to be implemented by a command class.
 */
public interface ISierraCommand {

    /**
     * This method processes the given inputs.
     *
     * @param user            the user initiating the process
     * @param sierraUser      the Sierra user associated with the process
     * @param abstractCommand the abstract command object used for processing
     * @param sierraLabel     the Sierra label used for the process
     * @param sierraArguments the arguments provided for the process
     */
    void process(User user, SierraUser sierraUser, IBukkitAbstractCommand abstractCommand,
                 ISierraLabel sierraLabel, ISierraArguments sierraArguments);

    /**
     * Generates a list of strings based on the given ID and arguments.
     *
     * @param id   the ID used to generate the list of strings
     * @param args an array of strings representing the arguments
     * @return a list of strings generated based on the ID and arguments
     */
    List<String> fromId(int id, String[] args);

    /**
     * Returns the description of the method or command.
     *
     * @return the description of the method or command as a String
     */
    String description();

    /**
     * Returns the permission required to execute the method or command.
     *
     * @return the required permission as a String
     */
    String permission();
}
