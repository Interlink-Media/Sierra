package de.feelix.sierra.check.impl.move;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.Arrays;
import java.util.Collections;

@SierraCheckData(checkType = CheckType.MOVEMENT_VALIDATION)
public class MovementValidation extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    private double lastChunkId = -1;
    private long lastTick = -1;
    private int buffer = 0;
    private Location lastLocation;
    private long lastTeleportTime = 0;
    private int deltaBuffer = 0;

    private static final double HARD_CODED_BORDER = 2.9999999E7D;
    private static final double SPECIAL_VALUE = 9.223372E18d;

    long timerBalanceRealTime = 0;
    long knownPlayerClockTime = (long) (System.nanoTime() - 6e10);
    long lastMovementPlayerClock = (long) (System.nanoTime() - 6e10);
    long clockDrift = (long) 120e6;
    long limitAbuseOverPing = 1000;
    boolean hasGottenMovementAfterTransaction = false;

    public MovementValidation(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData data) {
        if (!configEngine().config().getBoolean("prevent-protocol-move", true)) {
            return;
        }

        data.getTimingProcessor().getMovementTask().prepare();
        handleLatencyAbuse(event, data);

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            handleFlyingPacket(event, data);
        } else if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            handleVehicleMove(event, data);
        }

        data.getTimingProcessor().getMovementTask().end();
    }

    private void handleFlyingPacket(PacketReceiveEvent event, PlayerData playerData) {
        WrapperPlayClientPlayerFlying wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientPlayerFlying(event), playerData::exceptionDisconnect);

        if (wrapper.hasRotationChanged()) {
            checkInvalidRotation(wrapper, event);
        }

        if (!wrapper.hasPositionChanged()) return;

        Location location = wrapper.getLocation();
        playerData.setLastLocation(location);
        Vector3d position = location.getPosition();
        double chunkId = computeChunkId(position);

        if (lastLocation != null) checkDelta(position, event);

        handleChunkTravel(chunkId, event);
        lastChunkId = chunkId;

        checkForBorder(position, event);
        checkValue(event, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        lastLocation = location;
    }

    private void handleLatencyAbuse(PacketReceiveEvent event, PlayerData data) {
        if (!configEngine().config().getBoolean("prevent-timer-cheats", true)) {
            return;
        }

        if (hasGottenMovementAfterTransaction && checkForTransaction(event.getPacketType())) {
            knownPlayerClockTime = lastMovementPlayerClock;
            lastMovementPlayerClock = data.getTransactionProcessor().getPlayerClockAtLeast();
            hasGottenMovementAfterTransaction = false;
        }

        if (!shouldCountPacketForTimer(event.getPacketType())) return;

        hasGottenMovementAfterTransaction = true;
        timerBalanceRealTime += (long) 50e6;

        doCheck(event);
    }

    public void doCheck(final PacketReceiveEvent event) {
        final double transactionPing = getPlayerData().getTransactionProcessor().getTransactionPing();
        // Limit using transaction ping if over 1000ms (default)
        final boolean needsAdjustment = limitAbuseOverPing != -1 && transactionPing >= limitAbuseOverPing;
        final boolean wouldFailNormal = timerBalanceRealTime > System.nanoTime();
        final boolean failsAdjusted = needsAdjustment
                                      && (timerBalanceRealTime + ((transactionPing * 1e6) - clockDrift - 50e6))
                                         > System.nanoTime();
        if (wouldFailNormal || failsAdjusted) {
            if (wouldFailNormal) {

                long delay = System.nanoTime() - timerBalanceRealTime;
                double calculated = FormatUtils.calculateResult(delay);

                this.dispatch(event, ViolationDocument.builder()
                    .description("is moving invalid")
                    .mitigationStrategy(this.violations() > 45 ? MitigationStrategy.KICK : MitigationStrategy.MITIGATE)
                    .debugs(Collections.singletonList(
                        new Debug<>("Ticks", String.format("%.5f ticks ahead", Math.abs(calculated)))))
                    .build());
            }
            event.setCancelled(true);
            event.cleanUp();
            // Reset the violation by 1 movement
            timerBalanceRealTime -= (long) 50e6;
        }

        timerBalanceRealTime = Math.max(timerBalanceRealTime, lastMovementPlayerClock - clockDrift);
    }

    public boolean checkForTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
               packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }

    public boolean shouldCountPacketForTimer(PacketTypeCommon packetType) {
        // If not flying, or this was a teleport, or this was a duplicate 1.17 mojang stupidity packet
        return WrapperPlayClientPlayerFlying.isFlying(packetType);
    }

    private void handleVehicleMove(PacketReceiveEvent event, PlayerData data) {
        WrapperPlayClientVehicleMove wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientVehicleMove(event), data::exceptionDisconnect);

        Vector3d location = wrapper.getPosition();

        checkValue(event, location.getX(), location.getY(), location.getZ(), wrapper.getYaw(), wrapper.getPitch());
    }

    private void checkValue(PacketReceiveEvent event, double x, double y, double z, float yaw, float pitch) {
        if (invalidValue(x, y, z)) {
            this.dispatch(event, ViolationDocument.builder()
                .description("is moving invalid")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(
                    new Debug<>("X", x),
                    new Debug<>("Y", y),
                    new Debug<>("Z", z),
                    new Debug<>("Tag", "Extreme Double")
                ))
                .build());
        }

        if (invalidValue(yaw, pitch)) {
            this.dispatch(event, ViolationDocument.builder()
                .description("is rotating invalid")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(
                    new Debug<>("Yaw", yaw),
                    new Debug<>("Pitch", pitch),
                    new Debug<>("Tag", "Extreme Float")
                ))
                .build());
        }
    }

    private void checkInvalidRotation(WrapperPlayClientPlayerFlying wrapper, PacketReceiveEvent event) {
        float pitch = wrapper.getLocation().getPitch();
        float yaw = wrapper.getLocation().getYaw();

        if (Math.abs(pitch) > 90.01 || isOutOfRange(yaw) || isOutOfRange(pitch) || yaw == SPECIAL_VALUE
            || pitch == SPECIAL_VALUE) {

            this.dispatch(event, ViolationDocument.builder()
                .description("is rotating invalid")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(
                    new Debug<>("Yaw", yaw),
                    new Debug<>("Pitch", pitch)
                ))
                .build());
        }
    }

    private void checkDelta(Vector3d position, PacketReceiveEvent event) {
        double deltaX = Math.abs(position.getX() - lastLocation.getX());
        double deltaY = Math.abs(position.getY() - lastLocation.getY());
        double deltaZ = Math.abs(position.getZ() - lastLocation.getZ());
        double deltaXZ = Math.hypot(deltaX, deltaZ);

        // Skip a second after server teleportation or after server join
        if (System.currentTimeMillis() - lastTeleportTime <= 1000
            || System.currentTimeMillis() - getPlayerData().getJoinTime() <= 1000) return;

        if (deltaXZ > 7 && getPlayerData().getGameMode() == GameMode.SURVIVAL) {
            if (++deltaBuffer > 10) {
                this.dispatch(event, ViolationDocument.builder()
                    .description("is moving invalid")
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .debugs(Arrays.asList(
                        new Debug<>("DeltaXZ", deltaXZ),
                        new Debug<>("Buffer", deltaBuffer),
                        new Debug<>("GameMode", getPlayerData().getGameMode().name())
                    ))
                    .build());
            }
        } else {
            deltaBuffer = Math.max(0, deltaBuffer - 1);
        }

        if (invalidDeltaValue(deltaX, deltaY, deltaZ)) {
            this.dispatch(event, ViolationDocument.builder()
                .description("is moving invalid")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(
                    new Debug<>("DeltaX", deltaX),
                    new Debug<>("DeltaY", deltaY),
                    new Debug<>("DeltaZ", deltaZ),
                    new Debug<>("GameMode", getPlayerData().getGameMode().name())
                ))
                .build());
        }
    }

    private void handleChunkTravel(double chunkId, PacketReceiveEvent event) {
        long tick = System.currentTimeMillis();

        if (chunkId == lastChunkId) return;

        long travelTime = tick - lastTick;
        processBufferAndViolation(travelTime, event);

        lastTick = tick;
    }

    private void processBufferAndViolation(long travelTime, PacketReceiveEvent event) {
        if (travelTime < 20) {
            if (++buffer > 5) {
                this.dispatch(event, ViolationDocument.builder()
                    .description("is moving invalid")
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .debugs(Arrays.asList(
                        new Debug<>("Chunks", buffer),
                        new Debug<>("Time", (travelTime / buffer)),
                        new Debug<>("GameMode", getPlayerData().getGameMode().name())
                    ))
                    .build());
            }
        } else {
            buffer = Math.max(0, buffer - 1);
        }
    }

    private void checkForBorder(Vector3d position, PacketReceiveEvent event) {
        if (Math.abs(position.getX()) > HARD_CODED_BORDER || Math.abs(position.getY()) > HARD_CODED_BORDER
            || Math.abs(position.getZ()) > HARD_CODED_BORDER) {

            this.dispatch(event, ViolationDocument.builder()
                .description("is moving invalid")
                .mitigationStrategy(MitigationStrategy.BAN)
                .debugs(Collections.singletonList(new Debug<>("Tag", "out of border")
                )).build());
        }
    }

    private boolean isOutOfRange(float value) {
        return value < -80000.0 || value > 80000.0;
    }

    private boolean invalidValue(double... values) {
        for (double v : values) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return true;
            }
        }
        return false;
    }

    private boolean invalidValue(float... values) {
        for (float v : values) {
            if (Float.isInfinite(v)) {
                return true;
            }
        }
        return false;
    }

    private boolean invalidDeltaValue(double... deltas) {
        for (double delta : deltas) {
            if (delta >= 10.0 && delta % 1.0 == 0.0 && delta > 1000.0) {
                return true;
            }
        }
        return false;
    }

    private double computeChunkId(Vector3d position) {
        return Math.floor(position.getX() / 32) + Math.floor(position.getZ() / 32);
    }

    @Override
    public void handle(PacketSendEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            lastTeleportTime = System.currentTimeMillis();
        }
    }
}
