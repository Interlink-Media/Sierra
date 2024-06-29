package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.Collections;
import java.util.List;

/**
 * The CreativeAnvil class represents an implementation of the ItemCheck interface that handles checks for anvil items.
 */
public class CreativeAnvil implements ItemCheck {

    @Override
    public Triple<String, MitigationStrategy, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                                          NBTCompound nbtCompound, PlayerData playerData) {

        // This prevents the creation of buggy anvils that crash the client when placed
        // https://bugs.mojang.com/browse/MC-82677
        if (clickedStack.getType() == ItemTypes.ANVIL) {
            if (clickedStack.getLegacyData() < 0 || clickedStack.getLegacyData() > 2) {
                if (!SierraDataManager.skipAnvilCheck) {
                    return new Triple<>("clicked on an invalid anvil", MitigationStrategy.BAN, Collections.singletonList(
                        new Debug<>("LegacyData", clickedStack.getLegacyData())));
                }
            }
        }

        if (nbtCompound.getTags().containsKey("id")) {
            String id = nbtCompound.getStringTagValueOrNull("id");

            if (id == null) return null;

            if (id.contains("anvil")) {
                if (nbtCompound.getTags().containsKey("Damage")) {
                    NBTNumber damage = nbtCompound.getNumberTagOrNull("Damage");

                    if (damage == null) return null;

                    if (damage.getAsInt() > 3 || damage.getAsInt() < 0) {
                        return new Triple<>(
                            "clicked on an invalid anvil", MitigationStrategy.BAN,
                            Collections.singletonList(new Debug<>("Damage", damage.getAsInt()))
                        );
                    }
                }
            }
        }
        return null;
    }
}
