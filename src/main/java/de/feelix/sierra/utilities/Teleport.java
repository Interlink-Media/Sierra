package de.feelix.sierra.utilities;

import com.github.retrooper.packetevents.util.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Teleport {

    private final int teleportId;
    private final Vector3d position;
    private final float yaw;
    private final float pitch;
    private final long timestamp = System.currentTimeMillis();
}
