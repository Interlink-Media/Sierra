package de.feelix.sierra.check;

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

    private final List<SierraCheck> packetChecks = new ArrayList<>();
    private final PlayerData        playerData;

    public CheckManager(PlayerData playerData) {
        this.playerData = playerData;
        packetChecks.add(new FrequencyDetection(playerData));
        packetChecks.add(new BookValidation(playerData));
        packetChecks.add(new ProtocolValidation(playerData));
        packetChecks.add(new MovementValidation(playerData));
        packetChecks.add(new CreativeCrasher(playerData));
        packetChecks.add(new CommandValidation(playerData));
    }

    @Override
    public List<SierraCheck> availableChecks() {
        return new ArrayList<>(packetChecks);
    }
}
