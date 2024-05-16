package de.feelix.sierra.command.impl;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.impl.SierraCheck;
import de.feelix.sierraapi.commands.*;

import java.util.Collections;
import java.util.List;

public class InfoCommand implements ISierraCommand {

    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand, ISierraLabel sierraLabel,
                        ISierraArguments sierraArguments) {

        if (!validateArguments(sierraArguments)) {
            sendHelpSyntax(sierraSender);
            return;
        }

        String     playerName = sierraArguments.getArguments().get(1);
        PlayerData playerData = null;

        for (PlayerData value : Sierra.getPlugin().getSierraDataManager().getPlayerData().values()) {
            if (value.getUser().getName().equalsIgnoreCase(playerName)) {
                playerData = value;
                break;
            }
        }

        if (playerData == null) {
            sierraSender.getSender().sendMessage(Sierra.PREFIX + " §cNo player found");
            return;
        }

        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §7Information for §c" + playerName + "§7:");
        sierraSender.getSender()
            .sendMessage(Sierra.PREFIX + " §7Version: §c" + playerData.getClientVersion().getReleaseName());
        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §7Client: §c" + playerData.getBrand());
        sierraSender.getSender()
            .sendMessage(Sierra.PREFIX + " §7Ping: §c" + playerData.getPingProcessor().getPing() + " ms");
        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §7Game mode: §c" + playerData.getGameMode().name());
        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §7Ticks existed: §c" + playerData.getTicksExisted());
        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §c§lCheck information");

        long count = 0L;
        for (SierraCheck check : playerData.getCheckManager().availableChecks()) {
            if (check.violations() > 0) {
                count++;
            }
        }

        if(count == 0) {
            sierraSender.getSender().sendMessage(Sierra.PREFIX + " §cNo detections");
            return;
        }

        for (SierraCheck sierraCheck : playerData.getCheckManager().availableChecks()) {
            if(sierraCheck.violations() > 0) {
                sierraSender.getSender()
                    .sendMessage(Sierra.PREFIX + "  §8- §7" + sierraCheck.checkType().getFriendlyName() + ": §c"
                                 + sierraCheck.violations());
            }
        }
    }

    private void sendHelpSyntax(ISierraSender sierraSender) {
        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §cInvalid usage, try /sierra info <name>");
    }

    private boolean validateArguments(ISierraArguments sierraArguments) {
        return sierraArguments.getArguments().size() > 1;
    }

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

    @Override
    public String description() {
        return "Get info about player";
    }

    @Override
    public String permission() {
        return "sierra.command.info";
    }
}
