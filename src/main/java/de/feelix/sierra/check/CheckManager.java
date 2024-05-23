package de.feelix.sierra.check;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import de.feelix.sierra.check.impl.book.BookValidation;
import de.feelix.sierra.check.impl.command.CommandValidation;
import de.feelix.sierra.check.impl.creative.CreativeCrasher;
import de.feelix.sierra.check.impl.move.MovementValidation;
import de.feelix.sierra.check.impl.protocol.ProtocolValidation;
import de.feelix.sierra.check.impl.frequency.FrequencyDetection;
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
     * This map is used to manage packet checks for a player. The keys in the map represent the types of packet
     * checks, while the values are the corresponding instances of `Sierra
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
     * // Perform checks on player's packets
     * check.performCheck();
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
            .put(FrequencyDetection.class, new FrequencyDetection(playerData))
            .put(ProtocolValidation.class, new ProtocolValidation(playerData))
            .put(MovementValidation.class, new MovementValidation(playerData))
            .put(CreativeCrasher.class, new CreativeCrasher(playerData))
            .put(CommandValidation.class, new CommandValidation(playerData))
            .put(BookValidation.class, new BookValidation(playerData))
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
