package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

// PaperMC
public class FireworkSize implements ItemCheck {

    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack, NBTCompound nbtCompound) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

            if (wrapper.getItemStack().isPresent()) {
                if (this.invalid(wrapper.getItemStack().get())) {
                    return new Pair<>("Invalid explosion size in firework (place)", PunishType.BAN);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
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
