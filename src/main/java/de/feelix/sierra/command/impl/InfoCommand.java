package de.feelix.sierra.command.impl;

import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierraapi.check.impl.SierraCheck;
import de.feelix.sierraapi.commands.*;
import de.feelix.sierraapi.user.impl.SierraUser;

import java.util.Collections;
import java.util.List;

/**
 * The InfoCommand class implements the ISierraCommand interface and represents a command that retrieves and sends
 * player information.
 */
public class InfoCommand implements ISierraCommand {

    /**
     * Processes the command by retrieving player data and sending player information messages.
     *
     * @param user            the User object representing the user executing the command
     * @param sierraUser      the SierraUser object representing the user executing the command
     * @param abstractCommand the IBukkitAbstractCommand object representing the command being executed
     * @param sierraLabel     the ISierraLabel object representing the label of the command
     * @param sierraArguments the ISierraArguments object representing the arguments passed with the command
     */
    @Override
    public void process(User user, SierraUser sierraUser, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel,
                        ISierraArguments sierraArguments) {

        if (!validateArguments(sierraArguments)) {
            sendHelpSyntax(user);
            return;
        }

        String playerName = sierraArguments.getArguments().get(1);
        sendPlayerInfoMessages(user, playerName, sierraUser);
    }

    /**
     * Sends player information messages to a user.
     *
     * @param user       the User object representing the user receiving the messages
     * @param playerName the name of the player to send information about
     * @param sierraUser the SierraUser object representing the user executing the command
     */
    private void sendPlayerInfoMessages(User user, String playerName, SierraUser sierraUser) {
        user.sendMessage(Sierra.PREFIX + " §7Information for §c" + playerName + "§7:");
        sendUserData(user, sierraUser);
        sendCheckInformation(user, sierraUser);
    }

    /**
     * Sends user data messages to a user.
     *
     * @param user       the User object representing the user receiving the messages
     * @param sierraUser the SierraUser object representing the user executing the command
     */
    private void sendUserData(User user, SierraUser sierraUser) {
        user.sendMessage(Sierra.PREFIX + " §7Version: §c" + sierraUser.version());
        user.sendMessage(Sierra.PREFIX + " §7Client: §c" + sierraUser.brand());
        user.sendMessage(Sierra.PREFIX + " §7Ping: §c" + sierraUser.ping() + " ms");
        user.sendMessage(Sierra.PREFIX + " §7Game mode: §c" + sierraUser.gameMode().name());
        user.sendMessage(Sierra.PREFIX + " §7Ticks existed: §c" + sierraUser.ticksExisted());
        user.sendMessage(Sierra.PREFIX + " §c§lCheck information");
    }

    /**
     * Sends check information to a user.
     *
     * @param user       the User object representing the user receiving the information
     * @param sierraUser the SierraUser object representing the user executing the command
     */
    private void sendCheckInformation(User user, SierraUser sierraUser) {
        long count = getPlayerDetectionCount(sierraUser);
        if (count == 0) {
            user.sendMessage(Sierra.PREFIX + " §cNo detections");
            return;
        }
        for (SierraCheck sierraCheck : sierraUser.checkRepository().availableChecks()) {
            if (sierraCheck.violations() > 0) {
                user.sendMessage(
                    String.format("%s  §8- §7%s: §c%s", Sierra.PREFIX, sierraCheck.checkType().getFriendlyName(),
                                  sierraCheck.violations()
                    ));
            }
        }
    }

    /**
     * Returns the total number of violations found by all available checks in the given SierraUser object.
     *
     * @param sierraUser the SierraUser object representing the user
     * @return the total number of violations found by all available checks
     */
    private long getPlayerDetectionCount(SierraUser sierraUser) {
        long count = 0L;
        for (SierraCheck check : sierraUser.checkRepository().availableChecks()) {
            if (check.violations() > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Sends the help syntax message to the given user.
     *
     * @param user the User object representing the user to send the message to
     */
    private void sendHelpSyntax(User user) {
        user.sendMessage(Sierra.PREFIX + " §cInvalid usage, try /sierra info <name>");
    }

    /**
     * Validates the arguments passed with a command.
     *
     * @param sierraArguments an implementation of ISierraArguments representing the arguments passed with the command
     * @return true if the number of arguments is greater than 1, and false otherwise
     */
    private boolean validateArguments(ISierraArguments sierraArguments) {
        return sierraArguments.getArguments().size() > 1;
    }

    /**
     * Generate a list of strings based on the given ID and arguments.
     *
     * @param id   the ID used to generate the list of strings
     * @param args an array of strings representing the arguments
     * @return a list of strings generated based on the ID and arguments
     */
    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("info");
        } else if (id == 2 && args[0].equalsIgnoreCase("info")) {
            return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the description of the method or command.
     *
     * @return the description of the method or command as a String
     */
    @Override
    public String description() {
        return "Get info about player";
    }

    /**
     * Returns the permission required to execute the method or command.
     *
     * @return the required permission as a String
     */
    @Override
    public String permission() {
        return "sierra.command.info";
    }
}
