package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
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
 * The FireworkSize class is an implementation of the ItemCheck interface. It handles the check for an protocol
 * explosion
 * size in a firework. It checks if the explosion size in a firework exceeds the maximum allowed size and returns an
 * appropriate result.
 */
// PaperMC
public class FireworkSize implements ItemCheck {

    @Override
    public Triple<String, MitigationStrategy, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                                          NBTCompound nbtCompound, PlayerData playerData) {

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {

            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPlayerBlockPlacement(event),
                playerData::exceptionDisconnect
            );

            if (wrapper.getItemStack().isPresent()) {
                if (this.invalid(wrapper.getItemStack().get())) {
                    return new Triple<>(
                        "interacted with invalid firework", MitigationStrategy.BAN,
                        Collections.singletonList(new Debug<>("Type", "Place"))
                    );
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindow(event),
                playerData::exceptionDisconnect
            );
            if (wrapper.getCarriedItemStack() != null) {
                if (this.invalid(wrapper.getCarriedItemStack())) {
                    return new Triple<>(
                        "interacted with invalid firework", MitigationStrategy.BAN,
                        Collections.singletonList(new Debug<>("Type", "Click"))
                    );
                }
            }
        }

        if (invalid(clickedStack)) {
            return new Triple<>("interacted with invalid firework", MitigationStrategy.BAN, Collections.emptyList());
        }
        return null;
    }

    /**
     * Checks if an ItemStack has an protocol explosion size in a firework.
     * It checks if the explosion size exceeds the maximum allowed size.
     *
     * @param itemStack The ItemStack to check.
     * @return {@code true} if the explosion size is protocol, {@code false} otherwise.
     */
    private boolean invalid(ItemStack itemStack) {
        if (itemStack.getNBT() != null) {
            NBTCompound fireworkNBT = itemStack.getNBT().getCompoundTagOrNull("Fireworks");
            if (fireworkNBT != null) {
                NBTList<NBTCompound> explosionsNBT = fireworkNBT.getCompoundListTagOrNull("Explosions");

                if (explosionsNBT == null) return false;

                int maxExplosions = 25;
                return explosionsNBT.size() >= maxExplosions;
            }
        }
        return false;
    }
}
