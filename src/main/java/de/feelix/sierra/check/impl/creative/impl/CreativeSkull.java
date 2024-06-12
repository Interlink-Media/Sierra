package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.violation.PunishType;

import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * The CreativeSkull class implements the ItemCheck interface and represents a check for a creative skull item.
 * This class checks if the skull owner and properties of the clicked skull are valid.
 */
//Fixes crash head / glitch head
public class CreativeSkull implements ItemCheck {

    /**
     * This method handles the check for a given packet event, clicked item stack, NBT compound, and player data.
     *
     * @param event         the packet receive event
     * @param clickedStack  the clicked item stack
     * @param nbtCompound   the NBT compound
     * @param playerData    the player data
     * @return a pair of string and PunishType or null
     */
    @Override
    public Pair<String, PunishType> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                NBTCompound nbtCompound, PlayerData playerData) {
        if (nbtCompound == null) {
            return null;
        }

        if (!nbtCompound.getTags().containsKey("SkullOwner")) {
            return null;
        }

        NBTCompound skullOwner = nbtCompound.getCompoundTagOrNull("SkullOwner");
        if (skullOwner == null) {
            return new Pair<>("Contains protocol skull owner", PunishType.KICK);
        }

        if (skullOwner.getTags().containsKey("Id")) {
            try {
                //noinspection unused
                UUID uuid = UUID.fromString(Objects.requireNonNull(skullOwner.getStringTagValueOrNull("Id")));
            } catch (Exception e) {
                if(!SierraDataManager.skipSkullUUIDCheck) {
                    return new Pair<>("Unable to parse uuid", PunishType.MITIGATE);
                }
            }
        }

        if (skullOwner.getTags().containsKey("Properties")) {
            NBTCompound properties = skullOwner.getCompoundTagOrNull("Properties");
            if (properties == null) {
                return new Pair<>("Properties doesn't exist", PunishType.KICK);
            }

            NBTList<NBTCompound> textures = properties.getCompoundListTagOrNull("textures");
            if (textures == null) {
                return new Pair<>("Textures doesn't exist", PunishType.KICK);
            }

            for (int i = 0; i < textures.size(); i++) {
                NBTCompound texture = textures.getTag(i);
                if (texture == null) {
                    return new Pair<>("Texture tag doesn't exist", PunishType.KICK);
                }

                if (!texture.getTags().containsKey("Value")) {
                    return new Pair<>("Value tag doesn't exist", PunishType.KICK);
                }

                String value = texture.getStringTagValueOrNull("Value");
                String decoded;
                try {
                    decoded = new String(Base64.getDecoder().decode(value));
                } catch (Exception e) {
                    return new Pair<>("Unable to decode Value", PunishType.KICK);
                }

                JsonObject jsonObject;
                try {
                    jsonObject = new Gson().fromJson(decoded, JsonObject.class);
                } catch (Exception e) {
                    return new Pair<>("Unable to decode JsonObject", PunishType.KICK);
                }

                if (!jsonObject.has("textures")) {
                    return new Pair<>("Texture field doesn't exist after decode", PunishType.KICK);
                }

                jsonObject = jsonObject.getAsJsonObject("textures");
                if (!jsonObject.has("SKIN")) {
                    return new Pair<>("SKIN field doesn't exist after decode", PunishType.KICK);
                }

                jsonObject = jsonObject.getAsJsonObject("SKIN");
                if (!jsonObject.has("url")) {
                    return new Pair<>("URL field doesn't exist after decode", PunishType.KICK);
                }

                String url = jsonObject.get("url").getAsString();
                if (url.trim().isEmpty()) {
                    return new Pair<>("URL is empty", PunishType.KICK);
                }

                if (!(url.startsWith("http://textures.minecraft.net/texture/") || url.startsWith(
                    "https://textures.minecraft.net/texture/"))) {
                    return new Pair<>("Invalid skin url", PunishType.KICK);
                }
            }
        }
        return null;
    }
}
