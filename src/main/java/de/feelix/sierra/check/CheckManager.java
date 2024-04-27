package de.feelix.sierra.check;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import de.feelix.sierra.check.impl.book.MassiveBook;
import de.feelix.sierra.check.impl.command.BlockedCommand;
import de.feelix.sierra.check.impl.creative.ItemDetectionRunner;
import de.feelix.sierra.check.impl.move.InvalidMoveDetection;
import de.feelix.sierra.check.impl.invalid.InvalidPacketDetection;
import de.feelix.sierra.check.impl.sign.SignDetection;
import de.feelix.sierra.check.impl.spam.PacketSpamDetection;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.impl.SierraCheck;
import lombok.Getter;
import de.feelix.sierraapi.check.CheckRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * The CheckManager class is responsible for managing packet checks for a player.
 */
@Getter
public class CheckManager implements CheckRepository {

    /**
     * The `packetChecks` variable is a `ClassToInstanceMap` that stores instances of `SierraDetection` classes.
     * It is a private final variable in the `CheckManager` class.
     * <p>
     * This map is used to manage packet checks for a player. The keys in the map represent the types of packet checks, while the values are the corresponding instances of `Sierra
     * Detection` classes.
     * <p>
     * Example usage:
     * <p>
     * // Create a new packet check manager for a specific player
     * PlayerData playerData = new PlayerData();
     * CheckManager checkManager = new CheckManager(playerData);
     * <p>
     * // Get the available checks
     * List<SierraCheck> availableChecks = checkManager.availableChecks();
     * <p>
     * // Iterate over the available checks
     * for (SierraCheck check : availableChecks) {
     *     // Perform checks on player's packets
     *     check.performCheck();
     * }
     */
    private final ClassToInstanceMap<SierraDetection> packetChecks;

    /**
     * The playerData variable represents the data associated with a player.
     * It is an instance of the PlayerData class.
     */
    private final PlayerData playerData;

    /**
     * CheckManager is a class that manages the packet checks for a player.
     */
    public CheckManager(PlayerData playerData) {
        this.playerData = playerData;

        packetChecks = new ImmutableClassToInstanceMap.Builder<SierraDetection>()
                .put(PacketSpamDetection.class, new PacketSpamDetection(playerData))
                .put(SignDetection.class, new SignDetection(playerData))
                .put(InvalidPacketDetection.class, new InvalidPacketDetection(playerData))
                .put(InvalidMoveDetection.class, new InvalidMoveDetection(playerData))
                .put(ItemDetectionRunner.class, new ItemDetectionRunner(playerData))
                .put(BlockedCommand.class, new BlockedCommand(playerData))
                .put(MassiveBook.class, new MassiveBook(playerData))
                .build();
    }

    /**
     * Retrieves a list of available checks from the CheckManager.
     *
     * @return a list of SierraCheck objects representing the available checks
     */
    @Override
    public List<SierraCheck> availableChecks() {
        return new ArrayList<>(packetChecks.values());
    }
}
