package de.feelix.sierra.check;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import de.feelix.sierra.check.impl.book.BookValidation;
import de.feelix.sierra.check.impl.command.CommandValidation;
import de.feelix.sierra.check.impl.creative.CreativeCrasher;
import de.feelix.sierra.check.impl.move.MovementValidation;
import de.feelix.sierra.check.impl.protocol.ProtocolValidation;
import de.feelix.sierra.check.impl.frequency.FrequencyDetection;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
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

    // Todo: Create anti-bot mechanism in next update

    public CheckManager(PlayerData playerData) {
        this.playerData = playerData;
        packetChecks.add(new FrequencyDetection(playerData));
        packetChecks.add(new BookValidation(playerData));
        packetChecks.add(new ProtocolValidation(playerData));
        packetChecks.add(new MovementValidation(playerData));
        packetChecks.add(new CreativeCrasher(playerData));
        packetChecks.add(new CommandValidation(playerData));
    }

    public void processAvailableChecksReceive(PacketReceiveEvent event) {
        for (SierraCheck availableCheck : packetChecks) {
            if (availableCheck instanceof IngoingProcessor) {
                ((IngoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }

    public void processAvailableChecksSend(PacketSendEvent event) {
        for (SierraCheck availableCheck : packetChecks) {
            if (availableCheck instanceof OutgoingProcessor) {
                ((OutgoingProcessor) availableCheck).handle(event, playerData);
            }
        }
    }

    @Override
    public List<SierraCheck> availableChecks() {
        return new ArrayList<>(packetChecks);
    }
}
