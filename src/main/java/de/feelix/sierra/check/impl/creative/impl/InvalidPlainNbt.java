package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.Collections;
import java.util.List;

/**
 * An implementation of the {@link ItemCheck} interface that checks for protocol NBT data in an ItemStack.
 */
@SuppressWarnings("DataFlowIssue")
public class InvalidPlainNbt implements ItemCheck {


    private Triple<String, MitigationStrategy, List<Debug<?>>> invalidNbt(ItemStack itemStack) {

        NBTCompound tag = itemStack.getNBT();

        if (tag != null) {
            Triple<String, MitigationStrategy, List<Debug<?>>> crashDetails = checkForSpawner(tag);
            if (crashDetails != null) {
                return crashDetails;
            }
            return checkValidMap(tag);
        }
        return null;
    }

    private Triple<String, MitigationStrategy, List<Debug<?>>> checkValidMap(NBTCompound nbtTag) {
        NBTNumber range = nbtTag.getNumberTagOrNull("range");
        int       maxMapRange = 15;
        if (range != null && (range.getAsInt() > maxMapRange || range.getAsInt() < 0)) {
            return new Triple<>(
                "interacted with invalid map", MitigationStrategy.BAN,
                Collections.singletonList(new Debug<>("Range", range.getAsInt()))
            );
        }
        return null;
    }

    private Triple<String, MitigationStrategy, List<Debug<?>>> checkForSpawner(NBTCompound tag) {
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

    private Triple<String, MitigationStrategy, List<Debug<?>>> makePunishablePair(String property, int value) {
        return new Triple<>(
            "interacted with an item with invalid property", MitigationStrategy.KICK,
            Collections.singletonList(new Debug<>(property, value))
        );
    }


    @Override
    public Triple<String, MitigationStrategy, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                                          NBTCompound nbtCompound, PlayerData playerData) {

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPlayerBlockPlacement(event),
                playerData::exceptionDisconnect
            );
            if (wrapper.getItemStack().isPresent()) {
                return invalidNbt(wrapper.getItemStack().get());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindow(event),
                playerData::exceptionDisconnect
            );
            return invalidNbt(wrapper.getCarriedItemStack());
        }
        return null;
    }
}
