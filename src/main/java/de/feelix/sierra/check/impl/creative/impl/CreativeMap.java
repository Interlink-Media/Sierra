package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.Collections;
import java.util.List;


//Fixes CrashMap exploit
public class CreativeMap implements ItemCheck {


    @Override
    public Triple<String, MitigationStrategy, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                                          NBTCompound nbtCompound, PlayerData playerData) {
        if (nbtCompound.getTags().containsKey("Decorations")) {
            NBTList<NBTCompound> decorations = nbtCompound.getCompoundListTagOrNull("Decorations");

            if (decorations == null) return null;

            for (int i = 0; i < decorations.size(); i++) {
                NBTCompound decoration = decorations.getTag(i);
                if (decoration.getTags().containsKey("type")) {
                    NBTByte nbtByte = decoration.getTagOfTypeOrNull("type", NBTType.BYTE.getNBTClass());
                    if (nbtByte == null) {
                        return new Triple<>(
                            "clicked on an invalid item", MitigationStrategy.BAN,
                            Collections.singletonList(new Debug<>("Deco Type", "null"))
                        );
                    } else if (nbtByte.getAsByte() < 0) {
                        return new Triple<>(
                            "clicked on an invalid item", MitigationStrategy.BAN,
                            Collections.singletonList(new Debug<>("Deco Byte", nbtByte.getAsByte()))
                        );
                    }
                }
            }
        }
        return null;
    }
}
