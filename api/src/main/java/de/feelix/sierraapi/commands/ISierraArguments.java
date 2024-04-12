package de.feelix.sierraapi.commands;

import java.util.List;

/**
 * The ISierraArguments interface represents the arguments passed with a command.
 */
public interface ISierraArguments {

    /**
     * This method returns the list of arguments passed with a command.
     *
     * @return The list of arguments as a List<String>.
     */
    List<String> getArguments();
}
