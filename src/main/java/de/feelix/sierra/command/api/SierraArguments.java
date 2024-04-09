package de.feelix.sierra.command.api;


import de.feelix.sierraapi.commands.ISierraArguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SierraArguments implements ISierraArguments {

    private final List<String> arguments = new ArrayList<>();

    public SierraArguments(String[] arguments) {
        this.arguments.addAll(Arrays.asList(arguments));
    }

    @Override
    public List<String> getArguments() {
        return arguments;
    }
}
