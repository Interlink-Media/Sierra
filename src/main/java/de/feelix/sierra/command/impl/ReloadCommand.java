package de.feelix.sierra.command.impl;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.impl.command.BlockedCommand;
import de.feelix.sierraapi.commands.*;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements ISierraCommand {

    /**
     * This method is called to process a reload command. It invalidates the cache
     * in the SierraConfigEngine, sets the prefix for all messages sent by Sierra, and sends
     * a success message to the sender.
     *
     * @param sierraSender    The sender of the command.
     * @param abstractCommand The abstract command being processed.
     * @param sierraLabel     The label of the command.
     * @param sierraArguments The arguments of the command.
     */
    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        // Remove all cached config files
        Sierra.getPlugin().getSierraConfigEngine().invalidateCache();

        // Reset prefix
        Sierra.getPlugin().setPrefix();

        // Reset blocked commands in BlockedCommand check
        BlockedCommand.disallowedCommands = Sierra.getPlugin()
            .getSierraConfigEngine().config().getStringList("disallowed-commands");

        Sierra.getPlugin().getSierraDiscordGateway().setup();

        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §fConfiguration reloaded §asuccessfully");
    }

    /**
     * Converts an id to a list of strings based on the given id and arguments.
     * @param id the id to convert
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
}
