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
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.PunishType;

// PaperMC
// net/minecraft/server/network/ServerGamePacketListenerImpl.java:515
// net/minecraft/server/network/ServerGamePacketListenerImpl.java:1283
@SierraCheckData(checkType = CheckType.MOVE)
public class InvalidMoveDetection extends SierraDetection implements IngoingProcessor {


    private static final double HARD_CODED_BORDER = 2.9999999E7D;

    private double lastChunkId = -1;
    private long   lastTick    = -1;
    private int    buffer      = 0;

    public InvalidMoveDetection(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData data) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-invalid-move", true)) {
            return;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {

            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

            Location location = wrapper.getLocation();

            if (wrapper.hasRotationChanged()) checkInvalidPitch(location, event);

            if (!wrapper.hasPositionChanged()) return;

            Vector3d position = location.getPosition();

            double chunkId = Math.floor(position.getX() / 32) + Math.floor(position.getZ() / 32);

            long tick = System.currentTimeMillis();

            if (chunkId != lastChunkId) {

                long travelTime = tick - this.lastTick;

                if (travelTime < 20) {
                    buffer++;

                    if (buffer > 5) {
                        this.violation(event, ViolationDocument.builder()
                            .punishType(PunishType.KICK)
                            .debugInformation(String.format("Traveled %d chunks in ~%dms", buffer, travelTime))
                            .build());
                    }
                } else {
                    buffer = Math.max(0, buffer - 1);
                }
                this.lastTick = tick;
            }
            this.lastChunkId = chunkId;

            checkForBorder(position, event);

            if (invalidValue(location.getX(), location.getY(), location.getZ())) {
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Extreme values: double")
                        .punishType(PunishType.BAN)
                        .build());
            }

            if (invalidValue(location.getYaw(), location.getPitch())) {
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Extreme values: float")
                        .punishType(PunishType.BAN)
                        .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            WrapperPlayClientVehicleMove wrapper = new WrapperPlayClientVehicleMove(event);

            Vector3d location = wrapper.getPosition();

            if (invalidValue(location.getX(), location.getY(), location.getZ())) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Extreme values: double")
                    .punishType(PunishType.BAN)
                    .build());
            }

            if (invalidValue(wrapper.getYaw(), wrapper.getPitch())) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Extreme values: float")
                    .punishType(PunishType.BAN)
                    .build());
            }
        }
    }

    public boolean invalidValue(float... value) {
        for (float v : value) {
            if (Float.isInfinite(v)) {
                return true;
            }
        }
        return false;
    }

    public boolean invalidValue(double... value) {
        for (double v : value) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return true;
            }
        }
        return false;
    }

    private void checkForBorder(Vector3d position, PacketReceiveEvent event) {
        if (Math.abs(position.getX()) > HARD_CODED_BORDER || Math.abs(position.getY()) > HARD_CODED_BORDER
            || Math.abs(position.getZ()) > HARD_CODED_BORDER) {

            violation(event, ViolationDocument.builder()
                .debugInformation("Moved out of border")
                .punishType(PunishType.BAN)
                .build());
        }
    }

    private void checkInvalidPitch(Location location, PacketReceiveEvent event) {
        float pitch = location.getPitch();
        if (Math.abs(pitch) > 90.01) {
            violation(event, ViolationDocument.builder()
                .debugInformation(String.format("Pitch at %.2f", Math.abs(pitch)))
                .punishType(PunishType.BAN)
                .build());
        }
    }
}
