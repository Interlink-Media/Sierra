package de.feelix.sierra.check.impl.frequency;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.init.impl.start.Ticker;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.MitigationStrategy;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

@SierraCheckData(checkType = CheckType.FREQUENCY)
public class FrequencyDetection extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    private int lastBookEditTick = 0;
    private int lastDropItemTick = 0;
    private int lastCraftRequestTick = 0;
    private int dropCount = 0;
    private int containerId = -1;

    long timerBalanceRealTime = 0;

    // Default value is real time minus max keep-alive time
    long knownPlayerClockTime = (long) (System.nanoTime() - 6e10);
    long lastMovementPlayerClock = (long) (System.nanoTime() - 6e10);

    // How long should the player be able to fall back behind their ping?
    // Default: 120 milliseconds
    long clockDrift = (long) (120.0 * 1e6);
    long limitAbuseOverPing = 1000;

    boolean hasGottenMovementAfterTransaction = false;

    private final HashMap<PacketTypeCommon, Integer> packetCounts = new HashMap<>();

    public FrequencyDetection(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (!configEngine().config().getBoolean("prevent-packet-frequency", true)) {
            return;
        }

        playerData.getTimingProcessor().getFrequencyTask().prepare();
        PacketTypeCommon packetType = event.getPacketType();

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {

            YamlConfiguration configuration = Sierra.getPlugin().getSierraConfigEngine().config();
            if (configuration.getStringList("excluded-packets-from-limit").contains(packetType.getName())) return;

            long current = System.currentTimeMillis();

            packetCounts.merge(packetType, 1, Integer::sum);

            int limit = retrieveLimitFromConfiguration(packetType);
            int packetCount = packetCounts.getOrDefault(packetType, 0);

            if (packetCount > limit) {
                this.dispatch(event, ViolationDocument.builder()
                    .description("is sending packets too frequent")
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .debugs(Arrays.asList(
                        new Debug<>("Packet", packetType.getName()),
                        new Debug<>("Limit", limit),
                        new Debug<>("Count", packetCount),
                        new Debug<>("Alive", playerData.getPingProcessor().getPing()),
                        new Debug<>("Transaction", playerData.getTransactionProcessor().getTransactionPing()),
                        new Debug<>("Version", playerData.getClientVersion().getReleaseName()),
                        new Debug<>("Delay", (System.currentTimeMillis() - current) + "ms")
                    ))
                    .build());
                return;
            }
        } else {
            this.packetCounts.clear();
        }

        if (packetType.equals(PacketType.Play.Client.EDIT_BOOK)) {
            handleEditBook(event);
        } else if (packetType.equals(PacketType.Play.Client.PLUGIN_MESSAGE)) {
            handlePluginMessage(event, playerData);
        } else if (packetType.equals(PacketType.Play.Client.CRAFT_RECIPE_REQUEST)) {
            handleCraftRecipeRequest(event);
        } else if (packetType.equals(PacketType.Play.Client.PLAYER_DIGGING)) {
            handlePlayerDigging(event, playerData);
        }

        if (hasGottenMovementAfterTransaction && checkForTransaction(event.getPacketType())) {
            knownPlayerClockTime = lastMovementPlayerClock;
            lastMovementPlayerClock = playerData.getPlayerClockAtLeast();
            hasGottenMovementAfterTransaction = false;
        }

        if (!shouldCountPacketForTimer(event.getPacketType())) return;

        hasGottenMovementAfterTransaction = true;
        timerBalanceRealTime += (long) 50e6;

        doCheck(event);

        playerData.getTimingProcessor().getFrequencyTask().end();
    }

    // Check from: https://github.com/GrimAnticheat/Grim -> Credits to MWHunter
    private void doCheck(PacketReceiveEvent event) {

        final double transactionPing = getPlayerData().getTransactionPing();
        // Limit using transaction ping if over 1000ms (default)
        final boolean needsAdjustment = limitAbuseOverPing != -1 && transactionPing >= limitAbuseOverPing;
        final boolean wouldFailNormal = timerBalanceRealTime > System.nanoTime();
        final boolean failsAdjusted = needsAdjustment
                                      && (timerBalanceRealTime + ((transactionPing * 1e6) - clockDrift - 50e6))
                                         > System.nanoTime();
        if (wouldFailNormal || failsAdjusted) {

            double delay = (System.nanoTime() - timerBalanceRealTime) / (1000000000.0 / 20.0);

            this.dispatch(
                event, ViolationDocument.builder()
                    .description("is moving too frequent")
                    .mitigationStrategy(violations() > 75 ? MitigationStrategy.KICK : MitigationStrategy.MITIGATE)
                    .debugs(Arrays.asList(
                        new Debug<>("Version", getPlayerData().getClientVersion().getReleaseName()),
                        new Debug<>("Ping", getPlayerData().getPingProcessor().getPing() + "ms"),
                        new Debug<>("Desync", Math.abs(delay) + " ticks ahead"),
                        new Debug<>("Last Trans", (System.currentTimeMillis() - this.getPlayerData()
                            .getTransactionProcessor().lastTransReceived) + "ms"
                        )
                    ))
                    .build());

            // Reset the violation by 1 movement
            timerBalanceRealTime -= (long) 50e6;
        }

        timerBalanceRealTime = Math.max(timerBalanceRealTime, lastMovementPlayerClock - clockDrift);

    }

    public boolean shouldCountPacketForTimer(PacketTypeCommon packetType) {
        // If not flying, or this was a teleport, or this was a duplicate 1.17 mojang stupidity packet
        return WrapperPlayClientPlayerFlying.isFlying(packetType)
               && System.currentTimeMillis() - getPlayerData().getTeleportProcessor().getLastTeleportTime() > 1000;
    }

    public boolean checkForTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
               packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }

    private int retrieveLimitFromConfiguration(PacketTypeCommon packetType) {

        int limit = configEngine().config().getInt("generic-packet-frequency-default", 50);
        for (String string : configEngine().config().getStringList("generic-packet-frequency-limit")) {

            String[] parts = string.split(":");
            if (parts[0].equals(packetType.getName())) {
                limit = Integer.parseInt(parts[1]);
                break;
            }
        }
        return limit;
    }

    private void handleEditBook(PacketReceiveEvent event) {
        if (isSpamming(lastBookEditTick)) {
            this.dispatch(event, ViolationDocument.builder()
                .description("is editing books too frequent")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Collections.singletonList(new Debug<>("Tag", "BookEdit")))
                .build());
        }
    }

    private void handlePluginMessage(PacketReceiveEvent event, PlayerData playerData) {
        WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientPluginMessage(event), playerData::exceptionDisconnect);

        String channelName = wrapper.getChannelName();
        if (channelName.contains("MC|BEdit") || channelName.contains("MC|BSign")) {
            if (isSpamming(lastBookEditTick)) {
                this.dispatch(event, ViolationDocument.builder()
                    .description("is sending payloads too frequent")
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .debugs(Collections.singletonList(new Debug<>("Tag", "Payload")))
                    .build());
            }
        }
    }

    private void handleCraftRecipeRequest(PacketReceiveEvent event) {
        int currentTick = Ticker.getInstance().getCurrentTick();
        if (lastCraftRequestTick + 10 > currentTick) {
            this.dispatch(event, ViolationDocument.builder()
                .description("is requesting recipes too frequent")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Collections.singletonList(new Debug<>("Tag", "RecipeRequest")))
                .build());
            event.getUser().sendPacket(new WrapperPlayServerCloseWindow(this.containerId));
        } else {
            lastCraftRequestTick = currentTick;
        }
    }

    private void handlePlayerDigging(PacketReceiveEvent event, PlayerData playerData) {
        WrapperPlayClientPlayerDigging wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientPlayerDigging(event), playerData::exceptionDisconnect);

        if (wrapper.getAction() == DiggingAction.DROP_ITEM) {
            int currentTick = Ticker.getInstance().getCurrentTick();

            if (playerData.getGameMode() != GameMode.SPECTATOR) {
                if (lastDropItemTick != currentTick) {
                    dropCount = 0;
                    lastDropItemTick = currentTick;
                } else {
                    dropCount++;
                    if (dropCount >= 20) {
                        this.dispatch(event, ViolationDocument.builder()
                            .description("is digging too frequent")
                            .mitigationStrategy(MitigationStrategy.KICK)
                            .debugs(Collections.singletonList(new Debug<>("Tag", "Digging")))
                            .build());
                    }
                }
            }
        }
    }

    private boolean isSpamming(int lastActionTick) {
        int currentTick = Ticker.getInstance().getCurrentTick();
        boolean isSpamming = lastActionTick + 20 > currentTick;
        if (!isSpamming) {
            lastBookEditTick = currentTick;
        }
        return isSpamming;
    }

    @Override
    public void handle(PacketSendEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow window = CastUtil.getSupplier(
                () -> new WrapperPlayServerOpenWindow(event), playerData::exceptionDisconnect);
            this.containerId = window.getContainerId();
        }
    }
}
