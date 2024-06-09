package de.feelix.sierra.check.impl.move;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.PunishType;

@SierraCheckData(checkType = CheckType.MOVEMENT_VALIDATION)
public class MovementValidation extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    private double   lastChunkId      = -1;
    private long     lastTick         = -1;
    private int      buffer           = 0;
    private Location lastLocation;
    private long     lastTeleportTime = 0;
    private int      deltaBuffer      = 0;
    private long     lastFlyingTime   = 0L;
    private long     balance          = 0L;

    private static final double HARD_CODED_BORDER = 2.9999999E7D;
    private static final double SPECIAL_VALUE     = 9.223372E18d;
    private static final long   MAX_BAL           = 0;
    private static final long   BAL_RESET         = -50;
    private static final long   BAL_SUB_ON_TP     = 50;

    public MovementValidation(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData data) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-protocol-move", true)) {
            return;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            handleFlyingPacket(event, data);
            handleLatencyAbuse(event, data);
        } else if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            handleVehicleMove(event, data);
        }
    }

    private void handleFlyingPacket(PacketReceiveEvent event, PlayerData playerData) {
        WrapperPlayClientPlayerFlying wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientPlayerFlying(event), playerData::exceptionDisconnect);

        if (wrapper.hasRotationChanged()) {
            checkInvalidRotation(wrapper, event);
        }

        if (!wrapper.hasPositionChanged()) return;

        Location location = wrapper.getLocation();
        playerData.setLastLocation(location);
        Vector3d position = location.getPosition();
        double   chunkId  = computeChunkId(position);

        if (lastLocation != null) checkDelta(position, event);

        handleChunkTravel(chunkId, event);
        lastChunkId = chunkId;

        checkForBorder(position, event);
        checkExtremeValues(location, event);

        lastLocation = location;
    }

    private void handleVehicleMove(PacketReceiveEvent event, PlayerData data) {
        WrapperPlayClientVehicleMove wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientVehicleMove(event), data::exceptionDisconnect);

        Vector3d location = wrapper.getPosition();

        if (invalidValue(location.getX(), location.getY(), location.getZ())) {
            triggerViolation(event, "Extreme values: double", PunishType.KICK);
        }

        if (invalidValue(wrapper.getYaw(), wrapper.getPitch())) {
            triggerViolation(event, "Extreme values: float", PunishType.KICK);
        }
    }

    private void handleLatencyAbuse(PacketReceiveEvent event, PlayerData data) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-timer-cheats", true)) {
            return;
        }

        boolean noExempt = System.currentTimeMillis() - data.getJoinTime() > 1000;

        if (lastFlyingTime != 0L && noExempt) {
            long now = System.currentTimeMillis();
            balance += 50L;
            balance -= now - lastFlyingTime;
            if (balance > MAX_BAL) {
                triggerViolation(event, "Movement frequency: bal:~" + balance,
                                 violations() > 200 ? PunishType.KICK : PunishType.MITIGATE
                );
                balance = BAL_RESET;
            }
        }
        lastFlyingTime = System.currentTimeMillis();
    }

    private void checkInvalidRotation(WrapperPlayClientPlayerFlying wrapper, PacketReceiveEvent event) {
        float pitch = wrapper.getLocation().getPitch();
        float yaw   = wrapper.getLocation().getYaw();

        if (Math.abs(pitch) > 90.01 || isOutOfRange(yaw) || isOutOfRange(pitch) || yaw == SPECIAL_VALUE
            || pitch == SPECIAL_VALUE) {
            triggerViolation(event, String.format("Yaw: %.4f, Pitch: %.4f", yaw, pitch), PunishType.KICK);
        }
    }

    private void checkDelta(Vector3d position, PacketReceiveEvent event) {
        double deltaX  = Math.abs(position.getX() - lastLocation.getX());
        double deltaY  = Math.abs(position.getY() - lastLocation.getY());
        double deltaZ  = Math.abs(position.getZ() - lastLocation.getZ());
        double deltaXZ = Math.hypot(deltaX, deltaZ);

        if (System.currentTimeMillis() - lastTeleportTime <= 1000) return;

        if (deltaXZ > 7) {
            if (++deltaBuffer > 10) {
                triggerViolation(event, String.format("Invalid deltaXZ: %.2f", deltaXZ), PunishType.KICK);
            }
        } else {
            deltaBuffer = Math.max(0, deltaBuffer - 1);
        }

        if (invalidDeltaValue(deltaX, deltaY, deltaZ)) {
            triggerViolation(event, String.format("X: %.2f Y: %.2f Z: %.2f", deltaX, deltaY, deltaZ), PunishType.KICK);
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
                triggerViolation(
                    event, String.format("Traveled %d chunks in ~%dms", buffer, (travelTime / buffer)),
                    PunishType.KICK
                );
            }
        } else {
            buffer = Math.max(0, buffer - 1);
        }
    }

    private void checkExtremeValues(Location location, PacketReceiveEvent event) {
        if (invalidValue(location.getX(), location.getY(), location.getZ())) {
            triggerViolation(event, "Extreme values: double", PunishType.KICK);
        }

        if (invalidValue(location.getYaw(), location.getPitch())) {
            triggerViolation(event, "Extreme values: float", PunishType.KICK);
        }
    }

    private void checkForBorder(Vector3d position, PacketReceiveEvent event) {
        if (Math.abs(position.getX()) > HARD_CODED_BORDER || Math.abs(position.getY()) > HARD_CODED_BORDER
            || Math.abs(position.getZ()) > HARD_CODED_BORDER) {
            triggerViolation(event, "Moved out of border", PunishType.BAN);
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

    private void triggerViolation(PacketReceiveEvent event, String debugInformation, PunishType punishType) {
        violation(event, ViolationDocument.builder()
            .debugInformation(debugInformation)
            .punishType(punishType)
            .build());
    }

    @Override
    public void handle(PacketSendEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            lastTeleportTime = System.currentTimeMillis();
        }
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK
            || event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            balance -= BAL_SUB_ON_TP;
        }
    }
}
