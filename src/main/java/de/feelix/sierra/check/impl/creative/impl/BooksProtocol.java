package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.Collections;
import java.util.List;

/**
 * The BooksProtocol class implements the ItemCheck interface, which represents an item check. It handles the check
 * for books in the Sierra plugin.
 */
public class BooksProtocol implements ItemCheck {


    @Override
    public Triple<String, MitigationStrategy, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                                          NBTCompound nbtCompound, PlayerData playerData) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("disable-books-completely", false)) {
            return null;
        }

        if (clickedStack.getType() == ItemTypes.WRITTEN_BOOK || clickedStack.getType() == ItemTypes.WRITABLE_BOOK) {
            return new Triple<>("used an book, while disabled", MitigationStrategy.BAN, Collections.emptyList());
        }
        return null;
    }
}
