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
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.MitigationStrategy;
import org.bukkit.entity.Player;

import java.util.Collections;

@SierraCheckData(checkType = CheckType.POST)
public class PostCheck extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    public PostCheck(PlayerData playerData) {
        super(playerData);
    }

    private boolean sent = false;
    public long lastFlying, lastPacket;
    public double buffer = 0.0;

    private int exemptFromSwingingCheck = Integer.MIN_VALUE;

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        PacketTypeCommon packetType = event.getPacketType();

        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            final long now = System.currentTimeMillis();
            final long delay = now - this.lastPacket;

            if (this.sent) {
                if (delay > 40L && delay < 100L) {
                    this.buffer += 0.25;

                    if (this.buffer > 0.5) {
                        dispatch(event, ViolationDocument.builder()
                            .mitigationStrategy(
                                violations() > 50 ? MitigationStrategy.KICK : MitigationStrategy.MITIGATE)
                            .description("send packet post")
                            .debugs(Collections.singletonList(new Debug<>("Buffer", buffer)))
                            .build());
                    }
                } else {
                    this.buffer = Math.max(this.buffer - 0.025, 0);
                }
                this.sent = false;
            }

            this.lastFlying = now;
        } else if (packetType.equals(PacketType.Play.Client.PLAYER_ABILITIES)
                   || packetType.equals(PacketType.Play.Client.INTERACT_ENTITY)
                   || packetType.equals(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT)
                   || packetType.equals(PacketType.Play.Client.USE_ITEM)
                   || packetType.equals(PacketType.Play.Client.PLAYER_DIGGING)
                   || (packetType.equals(PacketType.Play.Client.CLICK_WINDOW) &&
                       getPlayerData().getClientVersion().isOlderThan(ClientVersion.V_1_13))
                   || (packetType.equals(PacketType.Play.Client.ANIMATION) && shouldHandleAnimation())
                   || (packetType.equals(PacketType.Play.Client.ENTITY_ACTION) && shouldHandleEntityAction(event))) {
            final long now = System.currentTimeMillis();
            final long delay = now - this.lastFlying;

            if (delay < 10L) {
                this.lastPacket = now;
                this.sent = true;
            } else {
                this.buffer = Math.max(this.buffer - 0.025, 0.0);
            }
        }
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

    private boolean isOlderServerVersion() {
        return PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8);
    }

    private boolean isSwingAnimation(WrapperPlayServerEntityAnimation animation) {
        return animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
               || animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND;
    }

    private boolean isRidingEntityInNewVersion() {
        return getPlayerData().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3)
               && ((Player) getPlayerData().getPlayer()).getVehicle() != null;
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
        }
    }
}
