package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * The CreativeMap class is a class that implements the ItemCheck interface. It is responsible for handling checks
 * related to a creative map exploit.
 * <p>
 * It contains a single method, handleCheck, which takes in a PacketReceiveEvent, an ItemStack, an NBTCompound, and a
 * PlayerData object. It checks if the NBTCompound contains a key called "Decorations". If it does, it retrieves the
 * NBTList of NBTCompounds associated with the "Decorations" key. It then iterates through each NBTCompound in the
 * list and checks if it contains a key called "type". If it does, it retrieves the value associated with the "type"
 * key as an NBTByte. If the NBTByte is null, it returns a Pair object with a message indicating that the NBT type field
 * is protocol and a PunishType of BAN. If the NBTByte is less than 0, it returns a Pair object with a message indicating
 * an protocol byte size in the map and a PunishType of BAN. If none of these conditions are met, it returns null.
 * <p>
 * This class is used to mitigate the exploit related to creative maps by checking the NBT data of clicked items in
 * order to identify any potential issues and take appropriate actions.
 */
//Fixes CrashMap exploit
public class CreativeMap implements ItemCheck {

    /**
     * This method handles the check for the given packet receive event, clicked stack, NBT compound, and player data.
     * It checks if the NBT compound contains the key "Decorations", and if so, iterates over the decorations list.
     * For each decoration, it checks if it contains the key "type". If the decoration does not have a valid NBT type
     * field, it returns a Pair object with an error message and the PunishType set to BAN. If the decoration has a
     * negative byte size, it returns a Pair object with an error message and the PunishType set to BAN. If all checks
     * pass, it returns null.
     *
     * @param event         The packet receive event
     * @param clickedStack  The clicked stack
     * @param nbtCompound   The NBT compound
     * @param playerData    The player data
     * @return A Pair object with an error message and the PunishType set to BAN if protocol NBT type field
     *                      or negative byte size is detected, null otherwise
     */
    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                NBTCompound nbtCompound, PlayerData playerData) {
        if (nbtCompound.getTags().containsKey("Decorations")) {
            NBTList<NBTCompound> decorations = nbtCompound.getCompoundListTagOrNull("Decorations");
            for (int i = 0; i < decorations.size(); i++) {
                NBTCompound decoration = decorations.getTag(i);
                if (decoration.getTags().containsKey("type")) {
                    NBTByte nbtByte = decoration.getTagOfTypeOrNull("type", NBTType.BYTE.getNBTClass());
                    if (nbtByte == null) {
                        return new Pair<>("Contains protocol nbt type field", PunishType.BAN);
                    } else if (nbtByte.getAsByte() < 0) {
                        return new Pair<>("Invalid byte size in map", PunishType.BAN);
                    }
                }
            }
        }
        return null;
    }
}
