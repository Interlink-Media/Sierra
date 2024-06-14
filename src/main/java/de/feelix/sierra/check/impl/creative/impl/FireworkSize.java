package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * The FireworkSize class is an implementation of the ItemCheck interface. It handles the check for an protocol explosion
 * size in a firework. It checks if the explosion size in a firework exceeds the maximum allowed size and returns an
 * appropriate result.
 */
// PaperMC
public class FireworkSize implements ItemCheck {

    /**
     * This method handles the check for an protocol explosion size in a firework.
     * It checks if the explosion size in a firework exceeds the maximum allowed size and returns a Pair<String, PunishType>.
     *
     * @param event          The PacketReceiveEvent that triggered the check.
     * @param clickedStack   The ItemStack that was clicked.
     * @param nbtCompound    The NBTCompound associated with the ItemStack.
     * @param playerData     The PlayerData of the player.
     * @return A Pair<String, PunishType> which represents the result of the check. If the explosion size is protocol, it returns a Pair with an error message and PunishType.BAN. Otherwise
     * , it returns null.
     */
    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack, NBTCompound nbtCompound, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {

            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPlayerBlockPlacement(event),
                playerData::exceptionDisconnect
            );

            if (wrapper.getItemStack().isPresent()) {
                if (this.invalid(wrapper.getItemStack().get())) {
                    return new Pair<>("Invalid explosion size in firework (place)", PunishType.BAN);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindow(event),
                playerData::exceptionDisconnect
            );
            if (wrapper.getCarriedItemStack() != null) {
                if (this.invalid(wrapper.getCarriedItemStack())) {
                    return new Pair<>("Invalid explosion size in firework (click)", PunishType.BAN);
                }
            }
        }

        if (invalid(clickedStack)) {
            return new Pair<>("Invalid explosion size in firework", PunishType.BAN);
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
