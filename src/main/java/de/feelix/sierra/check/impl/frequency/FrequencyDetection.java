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
import de.feelix.sierra.manager.init.impl.start.Ticker;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.entity.Player;

import java.util.HashMap;

@SierraCheckData(checkType = CheckType.FREQUENCY)
public class FrequencyDetection extends SierraDetection implements IngoingProcessor {

    private final HashMap<PacketTypeCommon, Double> multiplierMap        = new HashMap<>();
    private       int                               lastBookEditTick     = 0;
    private       int                               lastDropItemTick     = 0;
    private       int                               lastCraftRequestTick = 0;
    private       int                               dropCount            = 0;

    private int packets = 0;

    public FrequencyDetection(PlayerData playerData) {
        super(playerData);
        initializeMultiplierMap();
    }

    private void initializeMultiplierMap() {
        multiplierMap.put(PacketType.Play.Client.PLAYER_POSITION, 0.5);
        multiplierMap.put(PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, 0.5);
        multiplierMap.put(PacketType.Play.Client.PLAYER_ROTATION, 0.5);
        multiplierMap.put(PacketType.Play.Client.PLAYER_FLYING, 0.5);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-packet-frequency", true)) {
            return;
        }

        this.packets++;

        int packetLimit = Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getInt("generic-packet-frequency-limit", 150);

        if (packetLimit != -1 && this.packets > packetLimit) {
            violation(event, ViolationDocument.builder()
                .punishType(PunishType.KICK)
                .debugInformation("Generic Limit: " + this.packets + " > " + packetLimit)
                .build());
            event.cleanUp();
        }

        if (event.getPacketType() == PacketType.Play.Client.EDIT_BOOK) {
            handleEditBook(event);
        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            handlePluginMessage(event, playerData);
        } else if (event.getPacketType() == PacketType.Play.Client.CRAFT_RECIPE_REQUEST) {
            handleCraftRecipeRequest(event);
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            handlePlayerDigging(event, playerData);
        }

        playerData.getTransactionProcessor()
            .addRealTimeTask(playerData.getTransactionProcessor()
                                 .lastTransactionSent.get() + 1, () -> this.packets = 0);
        handlePacketAllowance(event, playerData);
    }

    private void handleEditBook(PacketReceiveEvent event) {
        if (isSpamming(lastBookEditTick)) {
            triggerViolation(event, "Spammed edit book", PunishType.KICK);
        }
    }

    private void handlePluginMessage(PacketReceiveEvent event, PlayerData playerData) {
        WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientPluginMessage(event), playerData::exceptionDisconnect);

        if (!(wrapper.getChannelName().contains("MC|BEdit") || wrapper.getChannelName().contains("MC|BSign"))) {
            return;
        }

        if (isSpamming(lastBookEditTick)) {
            triggerViolation(event, "Spammed payload", PunishType.KICK);
        }
    }

    private void handleCraftRecipeRequest(PacketReceiveEvent event) {
        int currentTick = Ticker.getInstance().getCurrentTick();
        if (lastCraftRequestTick + 10 > currentTick) {
            triggerViolation(event, "Spammed recipe request", PunishType.MITIGATE);
            //noinspection UnstableApiUsage
            ((Player) getPlayerData().getPlayer()).updateInventory();
        } else {
            lastCraftRequestTick = currentTick;
        }
    }

    private void handlePlayerDigging(PacketReceiveEvent event, PlayerData playerData) {
        WrapperPlayClientPlayerDigging wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientPlayerDigging(event), playerData::exceptionDisconnect);

        if (wrapper.getAction() != DiggingAction.DROP_ITEM) return;

        int currentTick = Ticker.getInstance().getCurrentTick();

        if (playerData.getGameMode() != GameMode.SPECTATOR) {
            if (lastDropItemTick != currentTick) {
                dropCount = 0;
                lastDropItemTick = currentTick;
            } else {
                dropCount++;
                if (dropCount >= 20) {
                    triggerViolation(event, "Spammed digging", PunishType.KICK);
                }
            }
        }
    }

    private void handlePacketAllowance(PacketReceiveEvent event, PlayerData playerData) {
        double multiplier      = multiplierMap.getOrDefault(event.getPacketType(), 1.0);
        double packetAllowance = playerData.getPacketAllowance();
        playerData.setPacketCount(playerData.getPacketCount() + (1 * multiplier));

        if (playerData.getPacketCount() > packetAllowance) {
            triggerViolation(
                event, String.format("Send: %s, allowed: %s", playerData.getPacketCount(), packetAllowance),
                PunishType.KICK
            );
        } else {
            playerData.setPacketAllowance(packetAllowance - 1);
        }
    }

    private boolean isSpamming(int lastActionTick) {
        int currentTick = Ticker.getInstance().getCurrentTick();
        if (lastActionTick + 20 > currentTick) {
            return true;
        } else {
            lastBookEditTick = currentTick;
            return false;
        }
    }

    private void triggerViolation(PacketReceiveEvent event, String debugInformation, PunishType punishType) {
        violation(event, ViolationDocument.builder()
            .debugInformation(debugInformation)
            .punishType(punishType)
            .build());
    }
}
