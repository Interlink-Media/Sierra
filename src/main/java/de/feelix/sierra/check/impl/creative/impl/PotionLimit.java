package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * The PotionLimit class implements the ItemCheck interface to handle custom potion effects.
 */
public class PotionLimit implements ItemCheck {

    /**
     * Handle the check for custom potion effects.
     *
     * @param event The packet receive event.
     * @param clickedStack The clicked item stack.
     * @param nbtCompound The NBT compound.
     * @param playerData The player data.
     * @return A Pair object containing the error message and the punishment type if a check fails, or null if all checks pass.
     */
    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                NBTCompound nbtCompound, PlayerData playerData) {
        if (!nbtCompound.getTags().containsKey("CustomPotionEffects")) {
            return null;
        }

        NBTList<NBTCompound> potionEffects = nbtCompound.getCompoundListTagOrNull("CustomPotionEffects");

        if (potionEffects == null) return null;

        int maxPotionEffects = 5;
        //Limit how many custom potion effects a potion can have
        if (potionEffects.size() >= maxPotionEffects) {
            return new Pair<>("Too big potion size", PunishType.KICK);
        }

        for (int i = 0; i < potionEffects.size(); i++) {
            NBTCompound effect = potionEffects.getTag(i);

            if (effect.getTags().containsKey("Duration")) {
                NBTNumber nbtNumber = effect.getNumberTagOrNull("Duration");
                if (nbtNumber != null) {
                    int maxEffectDuration = 9600;
                    if (nbtNumber.getAsInt() >= maxEffectDuration) {
                        return new Pair<>("Invalid potion duration", PunishType.KICK);
                    }
                }
            }

            if (effect.getTags().containsKey("Amplifier")) {
                //This is weird, in wiki it says this is a byte,
                //but trying to get the byte tag allows hacked clients to bypass this check for some reason
                //It flags however, if they attempt to open their inventory after creating the potion
                NBTNumber nbtNumber = effect.getNumberTagOrNull("Amplifier");
                if (nbtNumber != null) {
                    if (nbtNumber.getAsInt() < 0) {
                        return new Pair<>("Invalid Amplifier: NEG", PunishType.BAN);
                    }
                    int maxPotionEffectAmplifier = 10;
                    if (nbtNumber.getAsInt() > maxPotionEffectAmplifier) {
                        return new Pair<>("Invalid Amplifier: MAX", PunishType.KICK);
                    }
                }
            }

        }
        return null;
    }
}
