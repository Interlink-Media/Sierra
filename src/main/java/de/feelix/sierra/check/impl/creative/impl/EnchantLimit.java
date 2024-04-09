package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.utilities.CrashDetails;
import de.feelix.sierraapi.violation.PunishType;

public class EnchantLimit implements ItemCheck {
    private static final ClientVersion CLIENT_VERSION = PacketEvents.getAPI()
        .getServerManager()
        .getVersion()
        .toClientVersion();

    @Override
    public CrashDetails handleCheck(PacketReceiveEvent event, ItemStack clickedStack, NBTCompound nbtCompound) {
        //This is "version safe", since we check both the older 'ench' and the newer 'Enchantments' tag
        //Not a very clean approach. A way to get items within pe itemstacks would certainly be helpful
        if (nbtCompound.getTags().containsKey(clickedStack.getEnchantmentsTagName(CLIENT_VERSION))) {
            NBTList<NBTCompound> enchantments = nbtCompound.getCompoundListTagOrNull(
                clickedStack.getEnchantmentsTagName(CLIENT_VERSION));
            for (int i = 0; i < enchantments.size(); i++) {
                NBTCompound enchantment = enchantments.getTag(i);
                if (enchantment.getTags().containsKey("lvl")) {
                    NBTNumber number = enchantment.getNumberTagOrNull("lvl");
                    if (number.getAsInt() < 0 || number.getAsInt() > Sierra.getPlugin()
                        .getSierraConfigEngine()
                        .config()
                        .getInt("max-enchantment-level", 5)) {
                        return new CrashDetails("Invalid enchantment level", PunishType.KICK);
                    }
                }
            }
        }
        return null;
    }
}
