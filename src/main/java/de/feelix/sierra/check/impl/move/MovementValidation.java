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

    /**
     * Represents the hard-coded border value used for protocol move detection.
     *
     * <p>
     * The HARD_CODED_BORDER variable is a private constant in the InvalidMoveDetection class.
     * It is of type double and has a value of 2.9999999E7D.
     * </p>
     *
     * <p>
     * The hard-coded border value is used to check if a player's movement exceeds the valid boundary.
     * If the player's position is greater than the hard-coded border value, it is considered an protocol move.
     * </p>
     *
     * <p>
     * This variable is used in various methods within the InvalidMoveDetection class to handle and detect protocol
     * moves.
     * </p>
     */
    private static final double HARD_CODED_BORDER = 2.9999999E7D;

    /**
     * Represents the last chunk ID.
     *
     * <p>
     * The lastChunkId variable stores the ID of the last chunk that was processed.
     * It is used in the InvalidMoveDetection class to track the chunks that the player has moved through.
     * </p>
     *
     * @since 1.0
     */
    private double lastChunkId = -1;

    /**
     * Represents the timestamp of the last tick.
     *
     * <p>
     * The lastTick variable stores the timestamp of the last tick, which is a unit of time measurement used in the
     * game.
     * The value is initialized to -1, indicating that no tick has occurred yet.
     * </p>
     *
     * @see MovementValidation
     * @since 1.0
     */
    private long lastTick = -1;

    /**
     * Represents a private integer variable named buffer.
     * <p>
     * The buffer variable stores an integer value that is used within the class {@link MovementValidation}.
     * The purpose of this variable is not specified in the given code snippet.
     * </p>
     * <p>
     * This variable is declared with private access specifier, meaning it can only be accessed within the same class.
     * </p>
     * <p>
     * The initial value of the buffer variable is set to 0.
     * </p>
     * <p>
     * It is important to note that the buffer variable is not returned or used in any example code provided.
     * </p>
     *
     * @see MovementValidation
     * @since 1.0
     */
    private int buffer = 0;

    /**
     * Represents the last known location of an entity.
     * <p>
     * This variable stores the location object that represents the last known coordinates
     * of the entity. It is updated whenever the entity's location changes.
     * </p>
     * <p>
     * The location object contains information such as the world, coordinates (x, y, z),
     * and orientation (yaw, pitch) of the entity.
     * </p>
     *
     * @see Location
     */
    private Location lastLocation;

    /**
     * Constant representing a special value.
     * <p>
     * This constant is a final double value and acts as a special marker or predefined value.
     * <p>
     * The value of SPECIAL_VALUE is 9.223372E18d.
     */
    final double SPECIAL_VALUE = 9.223372E18d;

    /**
     * Represents the timestamp of the last teleport made by the player.
     * The timestamp is stored as a Unix timestamp in milliseconds.
     * A value of 0 indicates that no teleport has been made yet.
     */
    private long lastTeleportTime = 0;

    /**
     * Represents the buffer for tracking the change in position.
     * The deltaBuffer is used to store the difference between the current position and the previous position.
     *
     * <p>
     * This variable is used in the InvalidMoveDetection class to detect and handle protocol movement packets from a
     * player.
     * It is updated each time a movement packet is received and checked for any suspicious or protocol values.
     * </p>
     *
     * @since 1.0
     */
    private int deltaBuffer = 0;

    /**
     * Represents the last time the player was flying.
     */
    private long lastFlyingTime = 0L;

    /**
     * Represents the balance of a player.
     * <p>
     * The balance is used to track the movement frequency of the player. It is updated when the player is flying and
     * is decremented when the player is teleported. If the balance exceeds a certain threshold, a violation is created.
     */
    private long balance = 0L;

    /**
     * Represents the maximum balance of a player.
     * <p>
     * The maximum balance is used to determine the threshold at which a violation is created for movement frequency.
     * If the player's balance exceeds the maximum balance, a violation is generated.
     */
    private static final long maxBal = 0;

    /**
     * The balReset variable represents the balance reset value.
     *
     * <p>
     * This variable is a constant and is used to define the balance value at which a reset should occur.
     * </p>
     *
     * <p>
     * The value of balReset is -50.
     * </p>
     *
     * <p>
     * It is used in the TimerDetection class as one of the fields.
     * </p>
     *
     * @since 1.0
     */
    private static final long balReset = -50;

    /**
     * The balSubOnTp variable represents the balance decrease value when a player is teleported.
     * <p>
     * The balance is used to track the movement frequency of the player. It is updated when the player is flying and
     * is decremented when the player is teleported. If the balance
     * exceeds a certain threshold, a violation is created.
     */
    private static final long balSubOnTp = 50;

    /**
     * InvalidMoveDetection is a subclass of SierraDetection which is used to detect and handle protocol movement
     * packets from a player.
     * It examines the player's movement data and checks for any suspicious or protocol values.
     *
     * @param playerData The PlayerData object containing the player's data
     */
    public MovementValidation(PlayerData playerData) {
        super(playerData);
    }

    /**
     * This method handles the PacketReceiveEvent by checking for protocol movement and taking appropriate actions.
     *
     * @param event the PacketReceiveEvent triggered by receiving a packet from the player
     * @param data  the PlayerData object containing the player's data
     */
    @Override
    public void handle(PacketReceiveEvent event, PlayerData data) {

        if (isPreventInvalidMoveDisabled()) return;

        if (isFlying(event)) {
            getPlayerData().getTimingProcessor().getMovementTask().prepare();
            handleFlyingPacket(event, data);
            handleLatencyAbuse(event, data);
        } else if (isVehicleMovePacket(event)) {
            handleVehicleMovePacket(event, data);
        }
    }

    /**
     * This method handles abuse of latency in movement by checking for protocol values and taking appropriate actions.
     *
     * @param event the PacketReceiveEvent triggered by receiving a packet from the player
     * @param data  the PlayerData object containing the player's data
     */
    private void handleLatencyAbuse(PacketReceiveEvent event, PlayerData data) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-timer-cheats", true)) {
            return;
        }

        if (this.lastFlyingTime != 0L && System.currentTimeMillis() - data.getJoinTime() > 1000) {
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
     * Determines if the prevent-protocol-move feature is disabled.
     *
     * @return true if the prevent-protocol-move feature is disabled, false otherwise.
     */
    private boolean isPreventInvalidMoveDisabled() {
        return !Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-protocol-move", true);
    }

    /**
     * Determines if the given PacketReceiveEvent represents a flying packet.
     *
     * @param event the PacketReceiveEvent triggered by receiving a packet from the player
     * @return true if the packet type is flying, false otherwise
     */
    private boolean isFlying(PacketReceiveEvent event) {
        return WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
    }

    /**
     * Determines if the given {@link PacketReceiveEvent} represents a vehicle move packet.
     *
     * @param event the {@link PacketReceiveEvent} triggered by receiving a packet from the player
     * @return true if the packet type is {@code PacketType.Play.Client.VEHICLE_MOVE}, false otherwise
     */
    private boolean isVehicleMovePacket(PacketReceiveEvent event) {
        return event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE;
    }

    /**
     * This method handles the flying packet received from the player.
     *
     * @param event the PacketReceiveEvent triggered by receiving a packet from the player
     */
    private void handleFlyingPacket(PacketReceiveEvent event, PlayerData playerData) {

        WrapperPlayClientPlayerFlying wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientPlayerFlying(event), playerData::exceptionDisconnect);

        if (wrapper.hasRotationChanged()) {
            sortOutInvalidRotation(wrapper, event);
        }

        if (!wrapper.hasPositionChanged()) return;

        Location location = wrapper.getLocation();
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
     * Checks the delta (difference) between the current position and the previous position.
     *
     * @param position the current position of the entity
     * @param event    the PacketReceiveEvent triggered by receiving a packet from the player
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
     * Checks if the given deltas are protocol.
     *
     * @param deltas the delta values to check
     * @return true if any of the deltas are protocol, false otherwise
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
     * Sorts out protocol rotation data from a flying packet.
     *
     * @param wrapper the WrapperPlayClientPlayerFlying object containing the flying packet data
     * @param event   the PacketReceiveEvent triggered by receiving the packet from the player
     */
    private void sortOutInvalidRotation(WrapperPlayClientPlayerFlying wrapper, PacketReceiveEvent event) {
        if (wrapper.hasRotationChanged()) {
            checkInvalidPitch(wrapper.getLocation(), event);
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
     * Determines if the given value is out of range.
     *
     * @param value The value to check.
     * @return {@code true} if the value is out of range, {@code false} otherwise.
     */
    // Helper method to check if a value is out of range
    private boolean isOutOfRange(float value) {
        return value < (float) -80000.0 || value > (float) 80000.0;
    }

    /**
     * Sorts out traveled chunks based on the chunkId and the current system time.
     *
     * @param chunkId the ID of the chunk traveled to
     * @param event   the PacketReceiveEvent triggered by receiving a packet
     */
    private void sortOutTraveledChunks(double chunkId, PacketReceiveEvent event) {
        long tick = System.currentTimeMillis();

        if (chunkId == lastChunkId) return;

        long travelTime = tick - this.lastTick;
        processBufferAndViolation(travelTime, event);

        this.lastTick = tick;
    }

    /**
     * Process the buffer and handle violation if necessary.
     *
     * @param travelTime the travel time in milliseconds
     * @param event      the PacketReceiveEvent triggered by receiving a packet from the player
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
     * Sorts out extreme values from the given location.
     *
     * @param location the location to check for extreme values
     * @param event    the PacketReceiveEvent triggered by receiving a packet from the player
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
     * Builds a violation document with an extended value ban punishment type.
     *
     * @param debugInfo the debug information related to the violation document
     * @return the built violation document
     */
    private ViolationDocument buildExtValueKickViolation(String debugInfo) {
        return ViolationDocument.builder()
            .debugInformation(debugInfo)
            .punishType(PunishType.KICK)
            .build();
    }

    /**
     * This method handles a vehicle move packet received from the player.
     * It checks for protocol movement values and takes appropriate actions.
     *
     * @param event      The PacketReceiveEvent triggered by receiving the vehicle move packet from the player
     * @param playerData The PlayerData object containing the player's data
     */
    private void handleVehicleMovePacket(PacketReceiveEvent event, PlayerData playerData) {

        WrapperPlayClientVehicleMove wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientVehicleMove(event), playerData::exceptionDisconnect);

        Vector3d location = wrapper.getPosition();

        if (invalidValue(location.getX(), location.getY(), location.getZ())) {
            violation(event, buildExtValueKickViolation("Extreme values: double"));
        }

        if (invalidValue(wrapper.getYaw(), wrapper.getPitch())) {
            violation(event, buildExtValueKickViolation("Extreme values: float"));
        }
    }

    /**
     * Computes the chunk ID based on the given position.
     *
     * @param position the position of the entity
     * @return the computed chunk ID
     */
    private double computeChunkId(Vector3d position) {
        return Math.floor(position.getX() / 32) + Math.floor(position.getZ() / 32);
    }

    /**
     * Checks if any of the given float values is Infinite.
     *
     * @param value The float values to check for Infinity.
     * @return {@code true} if any of the values is Infinite, {@code false} otherwise.
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
     * Determines if any of the given double values is NaN or infinite.
     *
     * @param value The double values to check for NaN or infinite.
     * @return {@code true} if any of the values is NaN or infinite, {@code false} otherwise.
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
     * Check if the position is outside the border and handle violation accordingly.
     *
     * @param position The position to check
     * @param event    The event triggered by receiving a packet from the player
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
     * Checks for protocol pitch in the given location and handles violation if necessary.
     *
     * @param location The location to check for protocol pitch
     * @param event    The PacketReceiveEvent triggered by receiving a packet from the player
     */
    private void checkInvalidPitch(Location location, PacketReceiveEvent event) {
        float pitch = location.getPitch();
        if (Math.abs(pitch) > 90.01) {
            violation(event, ViolationDocument.builder()
                .debugInformation(String.format("Pitch at %.2f", Math.abs(pitch)))
                .punishType(PunishType.KICK)
                .build());
        }
    }

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
