package de.feelix.sierra.command.impl;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.commands.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * The AlertsCommand class represents a command that toggles the alert messages for a player.
 * It implements the ISierraCommand interface and provides the necessary methods to process the command.
 */
public class AlertsCommand implements ISierraCommand {

    /**
     * The process method performs the necessary actions to toggle the alert messages for a player.
     * It first retrieves the sender of the command as a Player using the provided sierraSender object.
     * If the sender is not a Player, the method returns.
     * It then retrieves the PlayerData object associated with the player using the getPlayerData method.
     * If the playerData is null, the method returns.
     * Finally, it calls the toggleAlertMessages method to toggle the alert messages for the player.
     *
     * @param sierraSender    the ISierraSender object representing the sender of the command
     * @param abstractCommand the IBukkitAbstractCommand object representing the executed command
     * @param sierraLabel     the ISierraLabel object representing the label of the initial symbol
     * @param sierraArguments the ISierraArguments object representing the arguments passed with the command
     */
    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        Player playerSender = sierraSender.getSenderAsPlayer();
        if (playerSender == null) return;

        PlayerData playerData = getPlayerData(playerSender);
        if (playerData == null) return;

        toggleAlertMessages(playerData, playerSender);
    }

    /**
     * Retrieves the PlayerData object associated with a player.
     *
     * @param playerSender the Player object representing the player
     * @return the PlayerData object associated with the player
     */
    private PlayerData getPlayerData(Player playerSender) {
        return Sierra.getPlugin().getSierraDataManager().getPlayerData(
            PacketEvents.getAPI().getPlayerManager().getUser(playerSender)
        ).get();
    }

    /**
     * Toggles the alert messages for a player.
     *
     * @param playerData   the PlayerData object representing the player
     * @param playerSender the Player object representing the player who executed the command
     */
    private void toggleAlertMessages(PlayerData playerData, Player playerSender) {
        if (playerData.isReceiveAlerts()) {
            playerData.setReceiveAlerts(false);
            sendMessage(playerSender, true);
        } else {
            playerData.setReceiveAlerts(true);
            sendMessage(playerSender, false);
        }
    }

    /**
     * Sends a message to the player indicating the status of the alert messages.
     *
     * @param player     the player to send the message to
     * @param isDisabled a boolean indicating whether the alert messages are disabled or not
     */
    private void sendMessage(Player player, boolean isDisabled) {
        String statusWord = isDisabled ? "§cdisabled" : "§aenabled";
        String message    = Sierra.PREFIX + " §fYou have " + statusWord + " §fthe alerts messages";
        player.sendMessage(message);
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
        return "Join the alerts-mode";
    }
}
