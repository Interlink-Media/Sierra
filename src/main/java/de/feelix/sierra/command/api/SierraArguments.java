package de.feelix.sierra.command.api;


import de.feelix.sierraapi.commands.ISierraArguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The SierraArguments class represents the arguments passed with a command. It implements the ISierraArguments interface.
 */
public class SierraArguments implements ISierraArguments {

    /**
     * The arguments field represents a list of string arguments passed with a command.
     * It is a private final field in the SierraArguments class, which implements the ISierraArguments interface.
     * The list is initialized as an empty ArrayList and can be modified by adding or removing elements.
     */
    private final List<String> arguments = new ArrayList<>();

    /**
     *
     * The SierraArguments class represents the arguments passed with a command. It implements the ISierraArguments interface.
     *
     * @param arguments an array of string arguments passed with a command
     */
    public SierraArguments(String[] arguments) {
        this.arguments.addAll(Arrays.asList(arguments));
    }

    /**
     * Retrieves the list of string arguments passed with a command.
     *
     * @return The list of string arguments
     */
    @Override
    public List<String> getArguments() {
        return arguments;
    }
}
