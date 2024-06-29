package de.feelix.sierra.check.impl.creative;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.List;

/**
 * An interface representing an item check.
 */
public interface ItemCheck {

    Triple<String, MitigationStrategy, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                                   NBTCompound nbtCompound, PlayerData playerData);
}
