package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * The BooksProtocol class implements the ItemCheck interface, which represents an item check. It handles the check
 * for books in the Sierra plugin.
 */
public class BooksProtocol implements ItemCheck {

    /**
     * Handles the check for books in the Sierra plugin.
     *
     * @param event         The PacketReceiveEvent that triggered the check.
     * @param clickedStack  The ItemStack that was clicked.
     * @param nbtCompound   The NBTCompound associated with the clickedStack.
     * @param playerData    The PlayerData of the player who clicked the ItemStack.
     * @return A Pair object containing a String message and a PunishType, or null if the check is disabled or no punishment is necessary.
     */
    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                NBTCompound nbtCompound, PlayerData playerData) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("disable-books-completely", false)) {
            return null;
        }

        if (clickedStack.getType() == ItemTypes.WRITTEN_BOOK || clickedStack.getType() == ItemTypes.WRITABLE_BOOK) {
            return new Pair<>("Using book", PunishType.BAN);
        }
        return null;
    }
}
