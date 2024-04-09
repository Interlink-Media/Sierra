package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.utilities.CrashDetails;
import de.feelix.sierraapi.violation.PunishType;

public class CreativeAnvil implements ItemCheck {

    //This prevents the creation of buggy anvils that crash the client when placed
    //https://bugs.mojang.com/browse/MC-82677

    private boolean invalid(ItemStack itemStack) {
        if (itemStack.getType() == ItemTypes.ANVIL) {
            return itemStack.getLegacyData() < 0 || itemStack.getLegacyData() > 2;
        }
        return false;
    }

    @Override
    public CrashDetails handleCheck(PacketReceiveEvent event, ItemStack clickedStack, NBTCompound nbtCompound) {
        if (invalid(clickedStack)) {
            return new CrashDetails("Invalid anvil meta", PunishType.BAN);
        }
        if (nbtCompound.getTags().containsKey("id")) {
            String id = nbtCompound.getStringTagValueOrNull("id");
            if (id.contains("anvil")) {
                if (nbtCompound.getTags().containsKey("Damage")) {
                    NBTNumber damage = nbtCompound.getNumberTagOrNull("Damage");
                    if(damage.getAsInt() > 3 || damage.getAsInt() < 0) {
                        return new CrashDetails("Invalid damage size", PunishType.BAN);
                    }
                }
            }
        }
        return null;
    }
}
