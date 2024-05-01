package de.feelix.sierra.check.impl.creative;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * An interface representing an item check.
 */
public interface ItemCheck {

    /**
     * Handles the check for an item.
     *
     * @param event         The PacketReceiveEvent that contains information about the event.
     * @param clickedStack  The ItemStack that was clicked by the player.
     * @param nbtCompound   The NBTCompound associated with the clickedStack.
     * @param playerData    The PlayerData object that contains information about the player.
     * @return A Pair<String, PunishType> object representing the result of the check. The first element of the pair is
     *          the message associated with the result of the check. The second element of the pair is the PunishType
     *          associated with the result of the check.
     */
    Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack, NBTCompound nbtCompound, PlayerData playerData);
}
