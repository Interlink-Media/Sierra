package de.feelix.sierra.command.impl;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.impl.SierraCheck;
import de.feelix.sierraapi.commands.*;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * The InfoCommand class implements the ISierraCommand interface and represents a command that retrieves and sends player information.
 */
public class InfoCommand implements ISierraCommand {

    /**
     * Processes the command to retrieve and send player information.
     *
     * @param sierraSender     the ISierraSender object used to send messages
     * @param abstractCommand  the IBukkitAbstractCommand object representing the command
     * @param sierraLabel      the ISierraLabel object representing the label of the command
     * @param sierraArguments  the ISierraArguments object representing the arguments passed with the command
     */
    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand, ISierraLabel sierraLabel,
                        ISierraArguments sierraArguments) {
        if (!validateArguments(sierraArguments)) {
            sendHelpSyntax(sierraSender);
            return;
        }
        String     playerName = sierraArguments.getArguments().get(1);
        PlayerData playerData = getPlayerData(playerName);
        if (playerData == null) {
            sierraSender.getSender().sendMessage(Sierra.PREFIX + " §cNo player found");
            return;
        }
        CommandSender sender = sierraSender.getSender();
        sendPlayerInfoMessages(sender, playerName, playerData);
    }

    /**
     * Retrieves the PlayerData object for the specified player name.
     *
     * @param playerName the name of the player
     * @return the PlayerData object for the specified player name, or null if not found
     */
    private PlayerData getPlayerData(String playerName) {
        for (PlayerData value : Sierra.getPlugin().getSierraDataManager().getPlayerData().values()) {
            if (value.getUser().getName().equalsIgnoreCase(playerName)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Sends player information messages to the provided command sender.
     *
     * @param sender     the command sender to send the player information messages to
     * @param playerName the name of the player to display in the messages
     * @param playerData the PlayerData object containing the player information
     */
    private void sendPlayerInfoMessages(CommandSender sender, String playerName, PlayerData playerData) {
        sender.sendMessage(Sierra.PREFIX + " §7Information for §c" + playerName + "§7:");
        sendUserData(sender, playerData);
        sendCheckInformation(sender, playerData);
    }

    /**
     * Sends user data information to the provided command sender.
     *
     * @param sender     the command sender to send the user data information to
     * @param playerData the PlayerData object containing the user data
     */
    private void sendUserData(CommandSender sender, PlayerData playerData) {
        sender.sendMessage(Sierra.PREFIX + " §7Version: §c" + playerData.getClientVersion().getReleaseName());
        sender.sendMessage(Sierra.PREFIX + " §7Client: §c" + playerData.getBrand());
        sender.sendMessage(Sierra.PREFIX + " §7Ping: §c" + playerData.getPingProcessor().getPing() + " ms");
        sender.sendMessage(Sierra.PREFIX + " §7Game mode: §c" + playerData.getGameMode().name());
        sender.sendMessage(Sierra.PREFIX + " §7Ticks existed: §c" + playerData.getTicksExisted());
        sender.sendMessage(Sierra.PREFIX + " §c§lCheck information");
    }

    /**
     * Sends check information to the provided sender.
     *
     * @param sender      the command sender to send the information to
     * @param playerData  the player data containing the check information
     */
    private void sendCheckInformation(CommandSender sender, PlayerData playerData) {
        long count = getPlayerDetectionCount(playerData);
        if (count == 0) {
            sender.sendMessage(Sierra.PREFIX + " §cNo detections");
            return;
        }
        for (SierraCheck sierraCheck : playerData.getCheckManager().availableChecks()) {
            if (sierraCheck.violations() > 0) {
                sender.sendMessage(Sierra.PREFIX + "  §8- §7" + sierraCheck.checkType().getFriendlyName() + ": §c"
                                   + sierraCheck.violations());
            }
        }
    }

    /**
     * Returns the count of player detections based on the provided PlayerData.
     *
     * @param playerData the PlayerData object representing the data of a player
     * @return the count of player detections as a long
     */
    private long getPlayerDetectionCount(PlayerData playerData) {
        long count = 0L;
        for (SierraCheck check : playerData.getCheckManager().availableChecks()) {
            if (check.violations() > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Sends the help syntax for an invalid command usage.
     *
     * @param sierraSender the ISierraSender object used to send the message
     */
    private void sendHelpSyntax(ISierraSender sierraSender) {
        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §cInvalid usage, try /sierra info <name>");
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
