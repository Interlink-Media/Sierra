package de.feelix.sierra.command.impl;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.commands.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AlertsCommand implements ISierraCommand {

    /**
     * This method is responsible for toggling the alerts messages for a player.
     *
     * @param sierraSender     the sender of the command
     * @param abstractCommand  the command being processed
     * @param sierraLabel      the label of the command
     * @param sierraArguments  the arguments of the command
     */
    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        Player playerSender = sierraSender.getSenderAsPlayer();

        if (playerSender == null) return;

        PlayerData playerData = Sierra.getPlugin()
            .getDataManager()
            .getPlayerData(PacketEvents.getAPI().getPlayerManager()
                               .getUser(playerSender)).get();

        if (playerData == null) return;

        if (playerData.isReceiveAlerts()) {
            playerData.setReceiveAlerts(false);
            playerSender.sendMessage(Sierra.PREFIX + " §fYou have §cdisabled §fthe alerts messages");
        } else {
            playerData.setReceiveAlerts(true);
            playerSender.sendMessage(Sierra.PREFIX + " §fYou have §aenabled §fthe alerts messages");
        }
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
