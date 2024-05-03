package de.feelix.sierra.command.impl;


import de.feelix.sierra.command.CommandHelper;
import de.feelix.sierraapi.commands.*;

import java.util.Collections;
import java.util.List;

public class VersionCommand implements ISierraCommand {

    /**
     * Processes the command by sending the version output to the sender.
     *
     * @param sierraSender    the ISierraSender object representing the sender
     * @param abstractCommand the IBukkitAbstractCommand object representing the command
     * @param sierraLabel     the ISierraLabel object representing the label
     * @param sierraArguments the ISierraArguments object representing the arguments
     */
    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        CommandHelper.sendVersionOutput(sierraSender);
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
}
