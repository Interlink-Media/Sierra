package de.feelix.sierra.check.impl.creative;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * An interface representing an item check.
 */
public interface ItemCheck {

    /**
     * Handles the check for an item.
     *
     * @param event          The PacketReceiveEvent representing the packet event.
     * @param clickedStack   The ItemStack representing the clicked item.
     * @param nbtCompound    The NBTCompound representing the NBT data of the item.
     * @return The CrashDetails representing crash details of a player.
     */
    Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack, NBTCompound nbtCompound);
}
