package de.feelix.sierra.command.impl;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.commands.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AlertsCommand implements ISierraCommand {

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

    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("alerts");
        }
        return Collections.emptyList();
    }

    @Override
    public String description() {
        return "Join the alerts-mode";
    }
}
