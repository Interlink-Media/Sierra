package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * The CreativeAnvil class represents an implementation of the ItemCheck interface that handles checks for anvil items.
 */
public class CreativeAnvil implements ItemCheck {

    /**
     * Determines whether the given ItemStack is an protocol anvil item.
     *
     * @param itemStack The ItemStack to check.
     * @return true if the given ItemStack is an protocol anvil item, false otherwise.
     */
    // This prevents the creation of buggy anvils that crash the client when placed
    // https://bugs.mojang.com/browse/MC-82677
    private boolean invalid(ItemStack itemStack) {
        if (itemStack.getType() == ItemTypes.ANVIL) {
            return itemStack.getLegacyData() < 0 || itemStack.getLegacyData() > 2;
        }
        return false;
    }

    /**
     * Handle the check for an anvil item.
     *
     * @param event         The PacketReceiveEvent that triggered the check.
     * @param clickedStack  The ItemStack that was clicked.
     * @param nbtCompound   The NBTCompound of the anvil item.
     * @param playerData    The PlayerData of the player.
     * @return A Pair object containing a String message and a PunishType, or null if the check passed.
     */
    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                NBTCompound nbtCompound, PlayerData playerData) {

        if (invalid(clickedStack) && !SierraDataManager.skipAnvilCheck) {
            return new Pair<>("Invalid anvil meta", PunishType.BAN);
        }
        if (nbtCompound.getTags().containsKey("id")) {
            String id = nbtCompound.getStringTagValueOrNull("id");

            if (id == null) return null;

            if (id.contains("anvil")) {
                if (nbtCompound.getTags().containsKey("Damage")) {
                    NBTNumber damage = nbtCompound.getNumberTagOrNull("Damage");

                    if (damage == null) return null;

                    if (damage.getAsInt() > 3 || damage.getAsInt() < 0) {
                        return new Pair<>("Invalid damage size", PunishType.BAN);
                    }
                }
            }
        }
        return null;
    }
}
