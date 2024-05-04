package de.feelix.sierra.check.impl.move;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.PunishType;

// PaperMC
// net/minecraft/server/network/ServerGamePacketListenerImpl.java:515
// net/minecraft/server/network/ServerGamePacketListenerImpl.java:1283
@SierraCheckData(checkType = CheckType.MOVE)
public class InvalidMoveDetection extends SierraDetection implements IngoingProcessor {

    /**
     * Represents the hard-coded border value used for invalid move detection.
     *
     * <p>
     * The HARD_CODED_BORDER variable is a private constant in the InvalidMoveDetection class.
     * It is of type double and has a value of 2.9999999E7D.
     * </p>
     *
     * <p>
     * The hard-coded border value is used to check if a player's movement exceeds the valid boundary.
     * If the player's position is greater than the hard-coded border value, it is considered an invalid move.
     * </p>
     *
     * <p>
     * This variable is used in various methods within the InvalidMoveDetection class to handle and detect invalid
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
     * @see InvalidMoveDetection
     * @since 1.0
     */
    private long lastTick = -1;

    /**
     * Represents a private integer variable named buffer.
     * <p>
     * The buffer variable stores an integer value that is used within the class {@link InvalidMoveDetection}.
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
     * @see InvalidMoveDetection
     * @since 1.0
     */
    private int buffer = 0;

    /**
     * InvalidMoveDetection is a subclass of SierraDetection which is used to detect and handle invalid movement
     * packets from a player.
     * It examines the player's movement data and checks for any suspicious or invalid values.
     *
     * @param playerData The PlayerData object containing the player's data
     */
    public InvalidMoveDetection(PlayerData playerData) {
        super(playerData);
    }

    /**
     * This method handles the PacketReceiveEvent by checking for invalid movement and taking appropriate actions.
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
        } else if (isVehicleMovePacket(event)) {
            handleVehicleMovePacket(event, data);
        }
    }

    /**
     * Determines if the prevent-invalid-move feature is disabled.
     *
     * @return true if the prevent-invalid-move feature is disabled, false otherwise.
     */
    private boolean isPreventInvalidMoveDisabled() {
        return !Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-invalid-move", true);
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

        sortOutInvalidRotation(wrapper, event);

        if (!wrapper.hasPositionChanged()) return;

        Location location = wrapper.getLocation();
        Vector3d position = location.getPosition();
        double   chunkId  = computeChunkId(position);

        sortOutTraveledChunks(chunkId, event);
        lastChunkId = chunkId;

        checkForBorder(position, event);
        sortOutExtremeValues(location, event);
        getPlayerData().getTimingProcessor().getMovementTask().end();
    }

    /**
     * Sorts out invalid rotation data from a flying packet.
     *
     * @param wrapper the WrapperPlayClientPlayerFlying object containing the flying packet data
     * @param event   the PacketReceiveEvent triggered by receiving the packet from the player
     */
    private void sortOutInvalidRotation(WrapperPlayClientPlayerFlying wrapper, PacketReceiveEvent event) {
        if (wrapper.hasRotationChanged()) checkInvalidPitch(wrapper.getLocation(), event);
    }

    /**
     * Sorts out traveled chunks based on the chunkId and the current system time.
     *
     * @param chunkId the ID of the chunk traveled to
     * @param event   the PacketReceiveEvent triggered by receiving a packet
     */
    private void sortOutTraveledChunks(double chunkId, PacketReceiveEvent event) {
        long tick = System.currentTimeMillis();
        if (chunkId == lastChunkId) {
            return;
        }

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
            violation(event, buildExtValueBanViolation("Extreme values: double"));
        }

        if (invalidValue(location.getYaw(), location.getPitch())) {
            violation(event, buildExtValueBanViolation("Extreme values: float"));
        }
    }

    /**
     * Builds a violation document with an extended value ban punishment type.
     *
     * @param debugInfo the debug information related to the violation document
     * @return the built violation document
     */
    private ViolationDocument buildExtValueBanViolation(String debugInfo) {
        return ViolationDocument.builder()
            .debugInformation(debugInfo)
            .punishType(PunishType.KICK)
            .build();
    }


    /**
     * This method handles a vehicle move packet received from the player.
     * It checks for invalid movement values and takes appropriate actions.
     *
     * @param event The PacketReceiveEvent triggered by receiving the vehicle move packet from the player
     * @param playerData The PlayerData object containing the player's data
     */
    private void handleVehicleMovePacket(PacketReceiveEvent event, PlayerData playerData) {

        WrapperPlayClientVehicleMove wrapper  = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientVehicleMove(event), playerData::exceptionDisconnect);

        Vector3d                     location = wrapper.getPosition();

        if (invalidValue(location.getX(), location.getY(), location.getZ())) {
            violation(event, buildExtValueBanViolation("Extreme values: double"));
        }

        if (invalidValue(wrapper.getYaw(), wrapper.getPitch())) {
            violation(event, buildExtValueBanViolation("Extreme values: float"));
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
     * Checks for invalid pitch in the given location and handles violation if necessary.
     *
     * @param location The location to check for invalid pitch
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
}
