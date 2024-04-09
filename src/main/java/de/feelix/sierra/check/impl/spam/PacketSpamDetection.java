package de.feelix.sierra.check.impl.spam;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Ticker;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;

@SierraCheckData(checkType = CheckType.SPAM)
public class PacketSpamDetection extends SierraDetection implements IngoingProcessor {

    public PacketSpamDetection(PlayerData playerData) {
        super(playerData);

        multiplierMap.put(PacketType.Play.Client.valueOf("PLAYER_POSITION"), 0.5);
        multiplierMap.put(PacketType.Play.Client.valueOf("PLAYER_POSITION_AND_ROTATION"), 0.5);
        multiplierMap.put(PacketType.Play.Client.valueOf("PLAYER_ROTATION"), 0.5);
        multiplierMap.put(PacketType.Play.Client.valueOf("PLAYER_FLYING"), 0.5);
        // multiplierMap.put(PacketType.Play.Client.valueOf("INTERACT_ENTITY"), 0.5); // beta
        multiplierMap.put(PacketType.Play.Client.valueOf("HELD_ITEM_CHANGE"), (double) 1);
        multiplierMap.put(PacketType.Play.Client.valueOf("ANIMATION"), (double) 1);
    }

    private final HashMap<PacketTypeCommon, Double> multiplierMap = new HashMap<>();

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-packet-spam", true)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.EDIT_BOOK) {
            if (invalid()) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Spammed edit book")
                    .punishType(PunishType.KICK)
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            // Make sure it's a book payload
            if (!(wrapper.getChannelName().contains("MC|BEdit") || wrapper.getChannelName().contains("MC|BSign"))) {
                return;
            }

            if (invalid()) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Spammed payload")
                    .punishType(PunishType.KICK)
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CRAFT_RECIPE_REQUEST) {
            int currentTick = Ticker.getInstance().getCurrentTick();
            if (getPlayerData().getLastCraftRequestTick() + 10 > currentTick) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Spammed recipe request")
                    .punishType(PunishType.MITIGATE)
                    .build());
                Player player = Bukkit.getPlayer(event.getUser().getUUID());
                if (player != null) {
                    //noinspection UnstableApiUsage
                    player.updateInventory();
                }
            } else {
                getPlayerData().setLastCraftRequestTick(currentTick);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);

            if (wrapper.getAction() != DiggingAction.DROP_ITEM) return;

            int currentTick = Ticker.getInstance().getCurrentTick();

            if (getPlayerData().getGameMode() != GameMode.SPECTATOR) {
                // limit how quickly items can be dropped
                // If the ticks aren't the same then the count starts from 0 and we update the lastDropTick.
                if (getPlayerData().getLastDropItemTick() != currentTick) {
                    getPlayerData().setDropCount(0);
                    getPlayerData().setLastDropItemTick(currentTick);
                } else {
                    // Else we increment the drop count and check the amount.
                    if (getPlayerData().dropCount++ >= 20) {
                        violation(event, ViolationDocument.builder()
                            .debugInformation("Spammed digging")
                            .punishType(PunishType.KICK)
                            .build());
                    }
                }
            }
        }

        double multiplier      = multiplierMap.getOrDefault(event.getPacketType(), 1.0D);
        double packetAllowance = getPlayerData().getPacketAllowance();

        if ((getPlayerData().packetCount += 1 * multiplier) > packetAllowance) {
            double packetCount = getPlayerData().getPacketCount();
            violation(event, ViolationDocument.builder()
                .debugInformation("Send: " + packetCount + ", allowed: " + packetAllowance)
                .punishType(PunishType.KICK)
                .build()
            );
        } else {
            getPlayerData().packetAllowance--;
        }
    }

    private boolean invalid() {
        int currentTick = Ticker.getInstance().getCurrentTick();
        if (getPlayerData().getLastBookEditTick() + 20 > currentTick) {
            return true;
        } else {
            getPlayerData().setLastBookEditTick(currentTick);
            return false;
        }
    }
}
