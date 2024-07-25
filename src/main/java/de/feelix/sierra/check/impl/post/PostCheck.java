package de.feelix.sierra.check.impl.post;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.EvictingQueue;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SierraCheckData(checkType = CheckType.POST)
public class PostCheck extends SierraDetection implements IngoingProcessor {

    public PostCheck(PlayerData playerData) {
        super(playerData);
    }

    private final ArrayDeque<PacketTypeCommon> postQueue = new ArrayDeque<>();
    private final List<String> flags = new EvictingQueue<>(10);
    private boolean hasSentFlyingPacket = false;

    private void handleFlyingPacket(PacketReceiveEvent event) {
        if (!flags.isEmpty() && configEngine().config().getBoolean("prevent-post-packets", true)) {

            // Okay, the user might be cheating, let's double check
            // 1.8 clients have the idle packet, and this shouldn't false on 1.8 clients
            // 1.9+ clients have predictions, which will determine if hidden tick skipping occurred

            long timeMillis = System.currentTimeMillis();

            boolean hasTeleported = timeMillis - getPlayerData().getTeleportProcessor().getLastTeleportTime() < 1000;
            boolean passedThreshold = timeMillis - getPlayerData().getJoinTime() > 1000 && !hasTeleported;

            if (passedThreshold) {
                for (String flag : flags) {
                    dispatch(event, ViolationDocument.builder()
                        .mitigationStrategy(violations() > 50 ? MitigationStrategy.KICK : MitigationStrategy.MITIGATE)
                        .description("send packet post")
                        .debugs(Collections.singletonList(new Debug<>("Packet", flag)))
                        .build());
                }
            }
            flags.clear();
        }

        postQueue.clear();
        hasSentFlyingPacket = true;
    }

    private void handleTransactionPacket() {
        if (hasSentFlyingPacket && !postQueue.isEmpty()) {
            flags.add(formatFlag(postQueue.getFirst()));
        }
        postQueue.clear();
        hasSentFlyingPacket = false;
    }

    private void handleOtherPackets(PacketTypeCommon packetType) {
        if (shouldQueuePostCheck(packetType)) {
            postQueue.add(packetType);
            getPlayerData().sendTransaction();
        }
    }

    private boolean shouldQueuePostCheck(PacketTypeCommon packetType) {
        return hasSentFlyingPacket && (
            packetType.equals(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT)
            || packetType.equals(PacketType.Play.Client.USE_ITEM)
            || (packetType.equals(PacketType.Play.Client.CLICK_WINDOW) && getPlayerData().getClientVersion()
                .isOlderThan(ClientVersion.V_1_13))
        );
    }

    private String formatFlag(PacketTypeCommon packetType) {
        return packetType.toString().toLowerCase(Locale.ROOT).replace("_", " ");
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        PacketTypeCommon packetType = event.getPacketType();

        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            handleFlyingPacket(event);
        } else if (isTransaction(packetType)) {
            handleTransactionPacket();
        } else {
            handleOtherPackets(packetType);
        }
    }

    public boolean isTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
               packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }
}