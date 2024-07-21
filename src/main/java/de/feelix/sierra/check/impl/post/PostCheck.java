package de.feelix.sierra.check.impl.post;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.EvictingQueue;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.MitigationStrategy;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SierraCheckData(checkType = CheckType.POST)
public class PostCheck extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    public PostCheck(PlayerData playerData) {
        super(playerData);
    }

    private final ArrayDeque<PacketTypeCommon> postQueue = new ArrayDeque<>();
    private final List<String> flags = new EvictingQueue<>(10);
    private boolean hasSentFlyingPacket = false;
    private int exemptFromSwingingCheck = Integer.MIN_VALUE;
    private long lastTeleportTime = 0;

    private void handleFlyingPacket(PacketReceiveEvent event) {
        if (!flags.isEmpty()) {
            // Okay, the user might be cheating, let's double check
            // 1.8 clients have the idle packet, and this shouldn't false on 1.8 clients
            // 1.9+ clients have predictions, which will determine if hidden tick skipping occurred
            if (System.currentTimeMillis() - this.lastTeleportTime > 1000) {
                for (String flag : flags) {
                    dispatch(event, ViolationDocument.builder()
                        .mitigationStrategy(MitigationStrategy.MITIGATE)
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
            String flag = formatFlag(postQueue.getFirst());
            flags.add(flag);
        }
        postQueue.clear();
        hasSentFlyingPacket = false;
    }

    private void handleOtherPackets(PacketTypeCommon packetType, PacketReceiveEvent event) {
        if (shouldQueuePostCheck(packetType, event)) {
            postQueue.add(packetType);
        }
    }

    private boolean shouldQueuePostCheck(PacketTypeCommon packetType, PacketReceiveEvent event) {
        return hasSentFlyingPacket && (
            (packetType.equals(PacketType.Play.Client.PLAYER_ABILITIES)
             || packetType.equals(PacketType.Play.Client.INTERACT_ENTITY)
             || packetType.equals(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT)
             || packetType.equals(PacketType.Play.Client.USE_ITEM)
             || packetType.equals(PacketType.Play.Client.PLAYER_DIGGING))
            || (packetType.equals(PacketType.Play.Client.CLICK_WINDOW) && getPlayerData().getClientVersion()
                .isOlderThan(ClientVersion.V_1_13))
            || (packetType.equals(PacketType.Play.Client.ANIMATION) && shouldHandleAnimation())
            || (packetType.equals(PacketType.Play.Client.ENTITY_ACTION) && shouldHandleEntityAction(event))
        );
    }

    private boolean shouldHandleAnimation() {
        return (getPlayerData().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) || isOlderServerVersion())
               && getPlayerData().getClientVersion().isOlderThan(ClientVersion.V_1_13)
               && exemptFromSwingingCheck < getPlayerData().getTransactionProcessor().lastTransactionReceived.get();
    }

    private boolean shouldHandleEntityAction(PacketReceiveEvent event) {
        WrapperPlayClientEntityAction entityAction = new WrapperPlayClientEntityAction(event);
        return getPlayerData().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
               || entityAction.getAction() != WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA
                  && !isRidingEntityInNewVersion();
    }

    private boolean isSwingAnimation(WrapperPlayServerEntityAnimation animation) {
        return animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
               || animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND;
    }

    private boolean isOlderServerVersion() {
        return PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8);
    }

    private boolean isRidingEntityInNewVersion() {
        return getPlayerData().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3)
               && ((Player) getPlayerData().getPlayer()).getVehicle() != null;
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
            handleOtherPackets(packetType, event);
        }
    }

    public boolean isTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
               packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }

    @Override
    public void handle(PacketSendEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_ANIMATION) {
            WrapperPlayServerEntityAnimation animation = new WrapperPlayServerEntityAnimation(event);
            if (animation.getEntityId() == playerData.entityId()) {
                if (isSwingAnimation(animation)) {
                    exemptFromSwingingCheck = getPlayerData().getTransactionProcessor().lastTransactionSent.get();
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            lastTeleportTime = System.currentTimeMillis();
        }
    }
}
