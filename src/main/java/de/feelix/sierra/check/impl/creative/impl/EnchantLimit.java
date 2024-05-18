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
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

/**
 * The EnchantLimit class is responsible for handling the check for valid enchantment levels on a clicked stack.
 * It implements the ItemCheck interface.
 */
public class EnchantLimit implements ItemCheck {

    private static final ClientVersion CLIENT_VERSION = PacketEvents.getAPI()
        .getServerManager()
        .getVersion()
        .toClientVersion();

    /**
     * Handles the check for valid enchantment levels on a clicked stack.
     *
     * @param event         The PacketReceiveEvent that triggered the check.
     * @param clickedStack  The ItemStack that was clicked.
     * @param nbtCompound   The NBTCompound associated with the clicked stack.
     * @param playerData    The PlayerData of the player who clicked the stack.
     * @return A Pair object containing the error message and the PunishType if the enchantment level is invalid, or null if the enchantment level is valid.
     */
    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                NBTCompound nbtCompound, PlayerData playerData) {

        //This is "version safe", since we check both the older 'ench' and the newer 'Enchantments' tag
        //Not a very clean approach. A way to get items within pe itemstacks would certainly be helpful
        if (nbtCompound.getTags().containsKey(clickedStack.getEnchantmentsTagName(CLIENT_VERSION))) {

            NBTList<NBTCompound> enchantments = nbtCompound.getCompoundListTagOrNull(
                clickedStack.getEnchantmentsTagName(CLIENT_VERSION));

            if(enchantments == null) return null;

            for (int i = 0; i < enchantments.size(); i++) {
                NBTCompound enchantment = enchantments.getTag(i);
                if (enchantment.getTags().containsKey("lvl")) {
                    NBTNumber number = enchantment.getNumberTagOrNull("lvl");
                    if (number.getAsInt() < 0 || number.getAsInt() > Sierra.getPlugin()
                        .getSierraConfigEngine()
                        .config()
                        .getInt("max-enchantment-level", 5)) {
                        return new Pair<>("Invalid enchantment level", PunishType.KICK);
                    }
                }
            }
        }
        return null;
    }
}
