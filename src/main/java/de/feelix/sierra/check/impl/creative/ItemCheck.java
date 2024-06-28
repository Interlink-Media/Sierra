package de.feelix.sierra.check.impl.creative;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.violation.PunishType;

import java.util.List;

/**
 * An interface representing an item check.
 */
public interface ItemCheck {


    /**
     * This method handles the check for a packet receive event.
     *
     * @param event        The packet receive event.
     * @param clickedStack The clicked stack item.
     * @param nbtCompound  The NBT compound associated with the clicked stack.
     * @param playerData   The player data for the player.
     * @return A triple containing the result of the check. The first element of the triple is a string representing
     * the result, the second element is a PunishType indicating the type of punishment, and the third element is
     * a list of Debug objects containing additional debug information.
     */
    Triple<String, PunishType, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                           NBTCompound nbtCompound, PlayerData playerData);
}
