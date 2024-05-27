package de.feelix.sierra.check.impl.frequency;

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
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.Ticker;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;

/**
 * This class is responsible for detecting packet spamming based on various multipliers.
 */
@SierraCheckData(checkType = CheckType.FREQUENCY)
public class FrequencyDetection extends SierraDetection implements IngoingProcessor {

    /**
     * The PacketSpamDetection class is responsible for detecting packet spamming based on various multipliers.
     */
    public FrequencyDetection(PlayerData playerData) {
        super(playerData);

        multiplierMap.put(PacketType.Play.Client.valueOf("PLAYER_POSITION"), 0.5);
        multiplierMap.put(PacketType.Play.Client.valueOf("PLAYER_POSITION_AND_ROTATION"), 0.5);
        multiplierMap.put(PacketType.Play.Client.valueOf("PLAYER_ROTATION"), 0.5);
        multiplierMap.put(PacketType.Play.Client.valueOf("PLAYER_FLYING"), 0.5);
    }

    /**
     * Private final field storing a HashMap that maps PacketTypeCommon to Double values.
     * This map is used to store multipliers for different packet types in a frequency detection system.
     * Each packet type corresponds to a specific Double value representing the multiplier.
     *
     * @see PacketTypeCommon
     */
    private final HashMap<PacketTypeCommon, Double> multiplierMap = new HashMap<>();

    /**
     * Tracks the tick at which the last book was edited.
     * <p>
     * The <code>lastBookEditTick</code> variable is used to store the tick number at which the last book was edited.
     * The tick number
     * is an integer value that represents the current game tick. This variable is primarily used in the context of
     * frequency detection
     * to check for book spamming by players. The value of <code>lastBookEditTick</code> is initialized to 0 by
     * default, indicating
     * that no book has been edited yet.
     * </p>
     * <p>
     * This variable is declared as private, which means it can only be accessed within the class it is declared in.
     * Other classes or
     * code outside the class cannot directly modify or read the value of <code>lastBookEditTick</code>. It is
     * recommended to use
     * getter and setter methods to access or modify the value of this variable.
     * </p>
     * <p>
     * Example usage:
     * </p>
     *
     * <pre>
     * {@code
     * private int lastBookEditTick = 0;
     * }
     * </pre>
     *
     * @see de.feelix.sierra.check.impl.frequency.FrequencyDetection
     */
    private int lastBookEditTick = 0;

    /**
     * Represents the tick value of the last dropped item.
     *
     * <p>
     * The lastDropItemTick variable is used to keep track of the tick value at which an item was last dropped.
     * It is typically used in frequency detection checks to determine the time elapsed since the last item drop.
     * </p>
     *
     * @see FrequencyDetection
     * @since 1.0
     */
    private int lastDropItemTick = 0;

    /**
     * Represents the tick value of the last craft request.
     * <p>
     * The value of this variable is used to track the tick at which the last craft request was made.
     * It is updated whenever a craft request event occurs.
     * <p>
     * Usage example:
     * <p>
     * ```java
     * int tick = lastCraftRequestTick;
     * ```
     *
     * @since 1.0
     */
    private int lastCraftRequestTick = 0;

    /**
     * Represents the count of dropped items in the game.
     * <p>
     * The dropCount variable is used to keep track of the number of items that have been dropped in the game.
     * It is incremented whenever an item is dropped and can be used for various purposes, such as detecting
     * excessive item drops or tracking player behavior.
     * <
     */
    private int dropCount = 0;

    /**
     * Handles the packet receive event and performs various checks to detect packet spamming.
     *
     * @param event      The packet receive event to handle
     * @param playerData The player data associated with the event
     */
    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-packet-frequency", true)) {
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

            WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPluginMessage(event), playerData::exceptionDisconnect);

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
            if (lastCraftRequestTick + 10 > currentTick) {
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
                lastCraftRequestTick = currentTick;
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {

            WrapperPlayClientPlayerDigging wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPlayerDigging(event), playerData::exceptionDisconnect);

            if (wrapper.getAction() != DiggingAction.DROP_ITEM) return;

            int currentTick = Ticker.getInstance().getCurrentTick();

            if (playerData.getGameMode() != GameMode.SPECTATOR) {
                // limit how quickly items can be dropped
                // If the ticks aren't the same then the count starts from 0 and we update the lastDropTick.
                if (lastDropItemTick != currentTick) {
                    dropCount = 0;
                    lastDropItemTick = currentTick;
                } else {
                    // Else we increment the drop count and check the amount.
                    dropCount++;
                    if (dropCount >= 20) {
                        violation(event, ViolationDocument.builder()
                            .debugInformation("Spammed digging")
                            .punishType(PunishType.KICK)
                            .build());
                    }
                }
            }
        }

        double multiplier      = multiplierMap.getOrDefault(event.getPacketType(), 1.0D);
        double packetAllowance = playerData.getPacketAllowance();
        playerData.setPacketCount(playerData.getPacketCount() + (1 * multiplier));

        if (playerData.getPacketCount() > packetAllowance) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Send: " + playerData.getPacketCount() + ", allowed: " + packetAllowance)
                .punishType(PunishType.KICK)
                .build()
            );
        } else {
            playerData.setPacketAllowance(packetAllowance - 1);
        }
    }

    /**
     * Checks if the current packet is considered protocol based on the last book edit tick.
     *
     * @return true if the packet is considered protocol, false otherwise
     */
    private boolean invalid() {
        int currentTick = Ticker.getInstance().getCurrentTick();
        if (lastBookEditTick + 20 > currentTick) {
            return true;
        } else {
            lastBookEditTick = currentTick;
            return false;
        }
    }
}
