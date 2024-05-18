package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * An implementation of the {@link ItemCheck} interface that checks for invalid NBT data in an ItemStack.
 */
public class InvalidPlainNbt implements ItemCheck {

    /**
     * Checks if a given ItemStack has invalid NBT data and returns the corresponding
     * punishable property and PunishType.
     *
     * @param itemStack the ItemStack to check for invalid NBT data
     * @return a Pair object representing the punishable property and PunishType, or null if there are no punishable properties
     */
    private Pair<String, PunishType> invalidNbt(ItemStack itemStack) {

        NBTCompound tag = itemStack.getNBT();

        if (tag != null) {
            Pair<String, PunishType> crashDetails = checkForSpawner(tag);
            if (crashDetails != null) {
                return crashDetails;
            }
            return checkValidMap(tag);
        }
        return null;
    }

    /**
     * Checks if a given NBTCompound tag contains a valid map and returns a Pair object representing the punishable property and the PunishType.
     *
     * @param nbtTag the NBTCompound tag to check
     * @return a Pair object representing the punishable property and the PunishType, or null if there are no punishable properties
     */
    private Pair<String, PunishType> checkValidMap(NBTCompound nbtTag) {
        NBTNumber range       = nbtTag.getNumberTagOrNull("range");
        int       maxMapRange = 15;
        if (range != null && (range.getAsInt() > maxMapRange || range.getAsInt() < 0)) {
            return new Pair<>("Range tag at " + range.getAsInt(), PunishType.BAN);
        }
        return null;
    }

    /**
     * Checks if a given NBTCompound tag contains punishable properties related to a spawner.
     *
     * @param tag the NBTCompound tag to check
     * @return a Pair object representing the punishable property and the PunishType, or null if there are no punishable properties
     */
    private Pair<String, PunishType> checkForSpawner(NBTCompound tag) {
        if (isPunishable(tag.getNumberTagOrNull("MaxNearbyEntities"), Byte.MAX_VALUE))
            return makePunishablePair("MaxNearbyEntities", tag.getNumberTagOrNull("MaxNearbyEntities").getAsInt());

        if (isPunishable(tag.getNumberTagOrNull("Delay"), Short.MAX_VALUE))
            return makePunishablePair("Delay", tag.getNumberTagOrNull("Delay").getAsInt());

        if (isPunishable(tag.getNumberTagOrNull("MinSpawnDelay"), Short.MAX_VALUE))
            return makePunishablePair("MinSpawnDelay", tag.getNumberTagOrNull("MinSpawnDelay").getAsInt());

        if (isPunishable(tag.getNumberTagOrNull("SpawnRange"), 20))
            return makePunishablePair("SpawnRange", tag.getNumberTagOrNull("SpawnRange").getAsInt());

        if (isPunishable(tag.getNumberTagOrNull("MaxSpawnDelay"), 1000))
            return makePunishablePair("MaxSpawnDelay", tag.getNumberTagOrNull("MaxSpawnDelay").getAsInt());

        if (isPunishable(tag.getNumberTagOrNull("SpawnCount"), 30))
            return makePunishablePair("SpawnCount", tag.getNumberTagOrNull("SpawnCount").getAsInt());

        if (isPunishable(tag.getNumberTagOrNull("RequiredPlayerRange"), 16))
            return makePunishablePair("RequiredPlayerRange", tag.getNumberTagOrNull("RequiredPlayerRange").getAsInt());

        return null;
    }

    /**
     * Determines if a given NBTNumber tag is punishable.
     *
     * @param tag      the NBTNumber tag to check
     * @param maxValue the maximum allowed value for the tag
     * @return true if the tag is punishable, false otherwise
     */
    private boolean isPunishable(NBTNumber tag, int maxValue) {
        return tag != null && (tag.getAsInt() > maxValue || tag.getAsInt() < 0);
    }

    /**
     * Creates a Pair object representing a punishable pair.
     *
     * @param property the property associated with the pair
     * @param value the value associated with the pair
     * @return a Pair object representing a punishable pair
     */
    private Pair<String, PunishType> makePunishablePair(String property, int value) {
        return new Pair<>(property + " at: " + value, PunishType.BAN);
    }

    /**
     * Handles the check based on the given event, clicked stack, NBT compound, and player data.
     *
     * @param event         the event representing the received packet
     * @param clickedStack  the clicked stack item
     * @param nbtCompound   the NBT compound associated with the item
     * @param playerData    the player data
     * @return a pair of strings and a PunishType if there is an invalid NBT compound, null otherwise
     */
    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                NBTCompound nbtCompound, PlayerData playerData) {

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPlayerBlockPlacement(event),
                playerData::exceptionDisconnect
            );
            if (wrapper.getItemStack().isPresent()) {
                return invalidNbt(wrapper.getItemStack().get());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientClickWindow(event),
                playerData::exceptionDisconnect
            );
            return invalidNbt(wrapper.getCarriedItemStack());
        }
        return null;
    }
}
