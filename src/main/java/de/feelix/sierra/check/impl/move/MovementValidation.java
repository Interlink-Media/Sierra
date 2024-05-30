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


// PaperMC
// net/minecraft/server/network/ServerGamePacketListenerImpl.java:515
// net/minecraft/server/network/ServerGamePacketListenerImpl.java:1283
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
    private static final long   maxBal            = 0;
    private static final long   balReset          = -50;
    private static final long   balSubOnTp        = 50;

    /**
     * Represents the MovementValidation class.
     * This class is responsible for validating movement data and performing various checks and actions.
     *
     * @param playerData The PlayerData object associated with the player.
     */
    public MovementValidation(PlayerData playerData) {
        super(playerData);
    }

    /**
     * Handles the received packet event by performing various checks and actions.
     *
     * @param event The PacketReceiveEvent associated with the received packet.
     * @param data  The PlayerData object associated with the player.
     */
    @Override
    public void handle(PacketReceiveEvent event, PlayerData data) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-protocol-move", true)) {
            return;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            getPlayerData().getTimingProcessor().getMovementTask().prepare();
            handleFlyingPacket(event, data);
            handleLatencyAbuse(event, data);

        } else if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {

            WrapperPlayClientVehicleMove wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientVehicleMove(event), data::exceptionDisconnect);

            Vector3d location = wrapper.getPosition();

            if (invalidValue(location.getX(), location.getY(), location.getZ())) {
                violation(event, buildExtValueKickViolation("Extreme values: double"));
            }

            if (invalidValue(wrapper.getYaw(), wrapper.getPitch())) {
                violation(event, buildExtValueKickViolation("Extreme values: float"));
            }
        }
    }

    /**
     * Handles the latency abuse by checking the flying time of the player and applying penalties if necessary.
     *
     * @param event The PacketReceiveEvent associated with the flying packet.
     * @param data  The PlayerData object associated with the player.
     */
    private void handleLatencyAbuse(PacketReceiveEvent event, PlayerData data) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-timer-cheats", true)) {
            return;
        }

        boolean noExempt = System.currentTimeMillis() - data.getJoinTime() > 1000;

        if (this.lastFlyingTime != 0L && noExempt) {
            final long now = System.currentTimeMillis();
            balance += 50L;
            balance -= now - lastFlyingTime;
            if (balance > maxBal) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Movement frequency: bal:~" + balance)
                    .punishType(violations() > 200 ? PunishType.KICK : PunishType.MITIGATE)
                    .build());
                balance = balReset;
            }
        }
        lastFlyingTime = System.currentTimeMillis();
    }

    /**
     * Handles the flying packet received from the client.
     *
     * @param event      The PacketReceiveEvent associated with the flying packet.
     * @param playerData The PlayerData object associated with the player.
     */
    private void handleFlyingPacket(PacketReceiveEvent event, PlayerData playerData) {

        WrapperPlayClientPlayerFlying wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientPlayerFlying(event), playerData::exceptionDisconnect);

        if (wrapper.hasRotationChanged()) {
            sortOutInvalidRotation(wrapper, event);
        }

        if (!wrapper.hasPositionChanged()) return;

        Location location = wrapper.getLocation();
        this.getPlayerData().setLastLocation(location);
        Vector3d position = location.getPosition();
        double   chunkId  = computeChunkId(position);

        if (this.lastLocation != null) checkDelta(position, event);

        sortOutTraveledChunks(chunkId, event);
        lastChunkId = chunkId;

        checkForBorder(position, event);
        sortOutExtremeValues(location, event);

        this.lastLocation = location;

        getPlayerData().getTimingProcessor().getMovementTask().end();
    }

    /**
     * Checks the delta values between the current position and the last recorded position,
     * and raises violation events if necessary.
     *
     * @param position The current position.
     * @param event    The PacketReceiveEvent associated with the position update.
     */
    private void checkDelta(Vector3d position, PacketReceiveEvent event) {

        double deltaX =
            Math.max(position.getX(), this.lastLocation.getX()) - Math.min(position.getX(), this.lastLocation.getX());
        double deltaY =
            Math.max(position.getY(), this.lastLocation.getY()) - Math.min(position.getY(), this.lastLocation.getY());
        double deltaZ =
            Math.max(position.getZ(), this.lastLocation.getZ()) - Math.min(position.getZ(), this.lastLocation.getZ());

        double deltaXZ = Math.hypot(deltaX, deltaZ);

        if (!(System.currentTimeMillis() - this.lastTeleportTime > 1000)) return;

        if (deltaXZ > 7) {
            this.deltaBuffer++;

            if (deltaBuffer++ > 10) {
                this.violation(event, ViolationDocument.builder()
                    .punishType(PunishType.KICK)
                    .debugInformation(String.format("Invalid deltaXZ: %.2f", deltaXZ))
                    .build());
            }
        } else {
            deltaBuffer = deltaBuffer > 0 ? deltaBuffer - 1 : 0;
        }

        if (invalidDeltaValue(deltaX, deltaY, deltaZ)) {
            violation(event, ViolationDocument.builder()
                .punishType(PunishType.KICK)
                .debugInformation(String.format("X: %.2f Y: %.2f Z: %.2f", deltaX, deltaY, deltaZ))
                .build());
        }
    }

    /**
     * Checks if any of the delta values provided are invalid according to the specified criteria.
     *
     * @param deltas The array of double values representing the delta values to check.
     * @return {@code true} if any of the delta values are greater than or equal to 10.0 and divisible by 1.0, and if
     * any delta value is greater than 1000.0; {@code false} otherwise
     * .
     */
    public boolean invalidDeltaValue(double... deltas) {
        for (double delta : deltas) {
            if (delta >= 10.0d && delta % 1.0d == 0.0d) {
                if (delta > 1000.0d) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sorts out any invalid rotation values and raises a violation event if necessary.
     *
     * @param wrapper The WrapperPlayClientPlayerFlying object containing player rotation information.
     * @param event   The PacketReceiveEvent associated with the player flying packet.
     */
    private void sortOutInvalidRotation(WrapperPlayClientPlayerFlying wrapper, PacketReceiveEvent event) {
        if (wrapper.hasRotationChanged()) {
            float pitch = wrapper.getLocation().getPitch();
            if (Math.abs(pitch) > 90.01) {
                violation(event, ViolationDocument.builder()
                    .debugInformation(String.format("Pitch at %.2f", Math.abs(pitch)))
                    .punishType(PunishType.KICK)
                    .build());
            }
        }

        float pitch = wrapper.getLocation().getPitch();
        float yaw   = wrapper.getLocation().getYaw();

        // Check if any condition is met
        if (isOutOfRange(yaw) || isOutOfRange(pitch) || yaw == SPECIAL_VALUE || pitch == SPECIAL_VALUE) {
            violation(event, ViolationDocument.builder()
                .debugInformation(String.format("Yaw: %.4f, Pitch: %.4f", yaw, pitch))
                .punishType(PunishType.KICK)
                .build());
        }
    }

    /**
     * Determines if the given value is outside the specified range.
     *
     * @param value The value to check.
     * @return {@code true} if the value is outside the range, {@code false} otherwise.
     */
    private boolean isOutOfRange(float value) {
        return value < (float) -80000.0 || value > (float) 80000.0;
    }

    /**
     * Sorts out traveled chunks based on the chunk ID and the current tick.
     *
     * @param chunkId The ID of the chunk to sort out.
     * @param event   The PacketReceiveEvent associated with the chunk sorting.
     */
    private void sortOutTraveledChunks(double chunkId, PacketReceiveEvent event) {
        long tick = System.currentTimeMillis();

        if (chunkId == lastChunkId) return;

        long travelTime = tick - this.lastTick;
        processBufferAndViolation(travelTime, event);

        this.lastTick = tick;
    }

    /**
     * Processes the buffer and raises a violation event if necessary.
     *
     * @param travelTime The travel time in milliseconds.
     * @param event      The PacketReceiveEvent that triggered the buffer processing.
     */
    private void processBufferAndViolation(long travelTime, PacketReceiveEvent event) {
        if (travelTime < 20) {
            buffer++;
            if (buffer > 5) {
                this.violation(event, ViolationDocument.builder()
                    .punishType(PunishType.KICK)
                    .debugInformation(String.format("Traveled %d chunks in ~%dms", buffer, (travelTime / buffer)))
                    .build());
            }
        } else {
            buffer = Math.max(0, buffer - 1);
        }
    }

    /**
     * Sorts out extreme values in the given location and raises a violation event if necessary.
     *
     * @param location The location to check for extreme values.
     * @param event    The PacketReceiveEvent that triggered the check.
     */
    private void sortOutExtremeValues(Location location, PacketReceiveEvent event) {
        if (invalidValue(location.getX(), location.getY(), location.getZ())) {
            violation(event, buildExtValueKickViolation("Extreme values: double"));
        }

        if (invalidValue(location.getYaw(), location.getPitch())) {
            violation(event, buildExtValueKickViolation("Extreme values: float"));
        }
    }

    /**
     * Builds a ViolationDocument with the given debug information and punishment type.
     *
     * @param debugInfo The debug information for the violation document.
     * @return The built ViolationDocument.
     */
    private ViolationDocument buildExtValueKickViolation(String debugInfo) {
        return ViolationDocument.builder()
            .debugInformation(debugInfo)
            .punishType(PunishType.KICK)
            .build();
    }

    /**
     * Computes the chunk ID based on the given position.
     *
     * @param position The position to compute the chunk ID for.
     * @return The computed chunk ID.
     */
    private double computeChunkId(Vector3d position) {
        return Math.floor(position.getX() / 32) + Math.floor(position.getZ() / 32);
    }

    /**
     * Determines if any of the given values is invalid.
     *
     * @param value The array of float values to check.
     * @return {@code true} if any of the values is NaN (Not-a-Number) or infinite, {@code false} otherwise.
     */
    public boolean invalidValue(float... value) {
        for (float v : value) {
            if (Float.isInfinite(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if any of the given values is invalid.
     *
     * @param value The array of double values to check.
     * @return {@code true} if any of the values is NaN (Not-a-Number) or infinite, {@code false} otherwise.
     */
    public boolean invalidValue(double... value) {
        for (double v : value) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given position is outside the border and handle violation event if so.
     *
     * @param position The position to check.
     * @param event    The PacketReceiveEvent that triggered the check.
     */
    private void checkForBorder(Vector3d position, PacketReceiveEvent event) {
        if (Math.abs(position.getX()) > HARD_CODED_BORDER
            || Math.abs(position.getY()) > HARD_CODED_BORDER
            || Math.abs(position.getZ()) > HARD_CODED_BORDER) {

            violation(event, ViolationDocument.builder()
                .debugInformation("Moved out of border")
                .punishType(PunishType.BAN)
                .build());
        }
    }

    /**
     * Handle method for processing a PacketSendEvent.
     *
     * @param event      The PacketSendEvent to handle.
     * @param playerData The PlayerData associated with the event.
     */
    @Override
    public void handle(PacketSendEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            this.lastTeleportTime = System.currentTimeMillis();
        }
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK
            || event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            balance -= balSubOnTp;
        }
    }
}
