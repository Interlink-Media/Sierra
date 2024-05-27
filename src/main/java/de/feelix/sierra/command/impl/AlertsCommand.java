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
public class AlertsCommand implements ISierraCommand {

    /**
     * This method processes the given parameters to perform a specific action.
     *
     * @param user            The User object representing the user.
     * @param sierraUser      The SierraUser object representing the user.
     * @param abstractCommand The IBukkitAbstractCommand object representing the command.
     * @param sierraLabel     The ISierraLabel object representing the label of the initial symbol.
     * @param sierraArguments The ISierraArguments object representing the arguments passed with the command.
     */
    @Override
    public void process(User user, SierraUser sierraUser, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        toggleAlertMessages(user, sierraUser);
    }

    /**
     * Toggles the alert messages for a given user.
     *
     * @param user       The User object representing the user.
     * @param sierraUser The SierraUser object representing the user.
     */
    private void toggleAlertMessages(User user, SierraUser sierraUser) {

        if (sierraUser.alertSettings().enabled()) {
            sendMessage(user, true);
            sierraUser.alertSettings().toggle(false);
        } else {
            sendMessage(user, false);
            sierraUser.alertSettings().toggle(true);
        }
    }

    /**
     * Sends a message to the user indicating the status of the alert messages.
     *
     * @param user       the user to send the message to
     * @param isDisabled a boolean indicating whether the alert messages are disabled or not
     */
    private void sendMessage(User user, boolean isDisabled) {
        user.sendMessage(new ConfigValue(
            "commands.alerts.toggle",
            "{prefix} &fYou have {status} &fthe alerts messages", true
        ).replacePrefix().replace("{status}", isDisabled ? "&cdisabled" : "&aenabled").colorize()
                             .message());
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
            return Collections.singletonList("alerts");
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
        return "Toggles the alerts";
    }

    @Override
    public String permission() {
        return "sierra.command.alerts";
    }
}
