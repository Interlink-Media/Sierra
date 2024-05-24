package de.feelix.sierra.command.impl;

import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.utilities.message.ConfigValue;
import de.feelix.sierraapi.commands.*;
import de.feelix.sierraapi.user.impl.SierraUser;

import java.util.Collections;
import java.util.List;

/**
 * The AlertsCommand class represents a command that toggles the alert messages for a player.
 * It implements the ISierraCommand interface and provides the necessary methods to process the command.
 */
public class MitigationCommand implements ISierraCommand {


    /**
     * This method processes the user, SierraUser, abstractCommand, sierraLabel, and sierraArguments
     * to toggle alert messages for a player.
     *
     * @param user            the User object representing the user
     * @param sierraUser      the SierraUser object representing the Sierra user
     * @param abstractCommand the IBukkiAbstractCommand object representing a wrapper around a Bukkit Command
     * @param sierraLabel     the ISierraLabel object representing the Sierra plugin label
     * @param sierraArguments the ISierraArguments object representing the arguments passed with a command
     */
    @Override
    public void process(User user, SierraUser sierraUser, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {
        toggleMitigationMessages(user, sierraUser);
    }

    /**
     * Toggles the mitigation messages for a user.
     *
     * @param user       the User object representing the user
     * @param sierraUser the SierraUser object representing the Sierra user
     */
    private void toggleMitigationMessages(User user, SierraUser sierraUser) {
        if (sierraUser.mitigationSettings().enabled()) {
            sendMessage(user, true);
            sierraUser.mitigationSettings().toggle(false);
        } else {
            sendMessage(user, false);
            sierraUser.mitigationSettings().toggle(true);
        }
    }

    /**
     * This method sends a message to a user indicating the status of the mitigation messages.
     *
     * @param user       the User object representing the user
     * @param isDisabled a boolean indicating whether the mitigation messages are disabled
     */
    private void sendMessage(User user, boolean isDisabled) {
        user.sendMessage(new ConfigValue(
            "commands.mitigation.toggle",
            "{prefix} &fYou have {status} &fthe mitigation messages", true
        )
                             .replacePrefix().replace("{status}", isDisabled ? "&cdisabled" : "&aenabled").colorize()
                             .getMessage());
    }

    /**
     * This method returns a list of strings from an id and an array of arguments.
     *
     * @param id   an integer representing the id
     * @param args an array of strings representing the arguments
     * @return a list of strings
     */
    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("mitigation");
        }
        return Collections.emptyList();
    }

    /**
     * This method returns the description of joining the alerts-mode.
     *
     * @return a string representing the description
     */
    @Override
    public String description() {
        return "Toggles the mitigations";
    }

    @Override
    public String permission() {
        return "sierra.command.mitigation";
    }
}
