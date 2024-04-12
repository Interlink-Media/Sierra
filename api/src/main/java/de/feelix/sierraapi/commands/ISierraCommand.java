package de.feelix.sierraapi.commands;

import java.util.List;

/**
 * ISierraCommand is an interface that represents a command in the Sierra command framework.
 * It defines the necessary methods that need to be implemented by a command class.
 */
public interface ISierraCommand {

    /**
     * This method is used to process a Sierra command. It executes the specified command when it is called.
     *
     * @param sierraSender an implementation of ISierraSender, representing the sender of the command
     * @param abstractCommand an implementation of IBukkitAbstractCommand, representing the command being executed
     * @param sierraLabel an implementation of ISierraLabel, representing the alias of the command used
     * @param sierraArguments an implementation of ISierraArguments, representing the arguments passed with the command
     */
    void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
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
}
