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

    private int lastBookEditTick     = 0;
    private int lastDropItemTick     = 0;
    private int lastCraftRequestTick = 0;
    private int dropCount            = 0;

    private final HashMap<PacketTypeCommon, Integer> count = new HashMap<>();

    public FrequencyDetection(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-packet-frequency", true)) {
            return;
        }

        long currented = System.currentTimeMillis();

        this.count.put(
            event.getPacketType(),
            this.count.containsKey(event.getPacketType()) ? this.count.get(event.getPacketType()) + 1 : 1
        );

        int limit = Sierra.getPlugin().getSierraConfigEngine()
            .config().getInt("generic-packet-frequency-default", 30);

        for (String string : Sierra.getPlugin()
            .getSierraConfigEngine().config().getStringList("generic-packet-frequency-limit")) {

            if (string.contains(event.getPacketType().getName())) {
                limit = Integer.parseInt(string.split(":")[1]);
            }
        }

        Integer packetCount = this.count.get(event.getPacketType());

        if (packetCount > limit) {
            violation(event, ViolationDocument.builder()
                .debugInformation(event.getPacketType().getName() + ", " + limit + "L, " + packetCount + "PPS, " + (
                    System.currentTimeMillis() - currented) + "ms")
                .punishType(PunishType.KICK)
                .build());
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
            .addRealTimeTask(
                playerData.getTransactionProcessor().lastTransactionSent.get() + 1,
                this.count::clear
            );
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
