package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.utilities.CrashDetails;
import de.feelix.sierraapi.violation.PunishType;

//Fixes CrashMap exploit
public class CreativeMap implements ItemCheck {

    @Override
    public CrashDetails handleCheck(PacketReceiveEvent event, ItemStack clickedStack, NBTCompound nbtCompound) {
        if (nbtCompound.getTags().containsKey("Decorations")) {
            NBTList<NBTCompound> decorations = nbtCompound.getCompoundListTagOrNull("Decorations");
            for (int i = 0; i < decorations.size(); i++) {
                NBTCompound decoration = decorations.getTag(i);
                if (decoration.getTags().containsKey("type")) {
                    NBTByte nbtByte = decoration.getTagOfTypeOrNull("type", NBTType.BYTE.getNBTClass());
                    if (nbtByte == null) {
                        return new CrashDetails("Contains invalid nbt type field", PunishType.BAN);
                    } else if(nbtByte.getAsByte() < 0) {
                        return new CrashDetails("Invalid byte size in map", PunishType.BAN);
                    }
                }
            }
        }
        return null;
    }
}
