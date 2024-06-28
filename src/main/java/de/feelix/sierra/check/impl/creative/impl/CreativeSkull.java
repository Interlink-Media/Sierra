package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.violation.PunishType;

import java.util.*;

/**
 * The CreativeSkull class implements the ItemCheck interface and represents a check for a creative skull item.
 * This class checks if the skull owner and properties of the clicked skull are valid.
 */
//Fixes crash head / glitch head
public class CreativeSkull implements ItemCheck {


    @Override
    public Triple<String, PunishType, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                                  NBTCompound nbtCompound, PlayerData playerData) {
        if (nbtCompound == null) {
            return null;
        }

        if (!nbtCompound.getTags().containsKey("SkullOwner")) {
            return null;
        }

        NBTCompound skullOwner = nbtCompound.getCompoundTagOrNull("SkullOwner");
        if (skullOwner == null) {
            return new Triple<>("clicked on skull with null owner", PunishType.KICK, Collections.emptyList());
        }

        if (skullOwner.getTags().containsKey("Id")) {
            try {
                //noinspection unused
                UUID uuid = UUID.fromString(Objects.requireNonNull(skullOwner.getStringTagValueOrNull("Id")));
            } catch (Exception e) {
                if (!SierraDataManager.skipSkullUUIDCheck) {
                    return new Triple<>("clicked on skull with unparsable uuid", PunishType.KICK,
                                        Collections.singletonList(new Debug<>("Exception", e.getMessage()))
                    );
                }
            }
        }

        if (skullOwner.getTags().containsKey("Properties")) {
            NBTCompound properties = skullOwner.getCompoundTagOrNull("Properties");
            if (properties == null) {
                return new Triple<>(
                    "clicked on skull with invalid properties", PunishType.KICK, Collections.emptyList());
            }

            NBTList<NBTCompound> textures = properties.getCompoundListTagOrNull("textures");
            if (textures == null) {
                return new Triple<>("clicked on skull with invalid textures", PunishType.KICK, Collections.emptyList());
            }

            for (int i = 0; i < textures.size(); i++) {
                NBTCompound texture = textures.getTag(i);
                if (texture == null) {
                    return new Triple<>(
                        "clicked on skull with invalid texture tag", PunishType.KICK, Collections.emptyList());
                }

                if (!texture.getTags().containsKey("Value")) {
                    return new Triple<>(
                        "clicked on skull with invalid value tag", PunishType.KICK, Collections.emptyList());
                }

                String value = texture.getStringTagValueOrNull("Value");
                String decoded;
                try {
                    decoded = new String(Base64.getDecoder().decode(value));
                } catch (Exception e) {
                    return new Triple<>(
                        "clicked on skull with invalid texture value", PunishType.KICK,
                        Collections.singletonList(new Debug<>("Exception", e.getMessage()))
                    );
                }

                JsonObject jsonObject;
                try {
                    jsonObject = new Gson().fromJson(decoded, JsonObject.class);
                } catch (Exception e) {
                    return new Triple<>(
                        "clicked on skull with invalid undecidable object", PunishType.KICK, Collections.emptyList());
                }

                if (!jsonObject.has("textures")) {
                    return new Triple<>(
                        "clicked on skull with no textures field", PunishType.KICK, Collections.emptyList());
                }

                jsonObject = jsonObject.getAsJsonObject("textures");
                if (!jsonObject.has("SKIN")) {
                    return new Triple<>(
                        "clicked on skull with no skin field", PunishType.KICK, Collections.emptyList());
                }

                jsonObject = jsonObject.getAsJsonObject("SKIN");
                if (!jsonObject.has("url")) {
                    return new Triple<>(
                        "clicked on skull with no url field", PunishType.KICK, Collections.emptyList());
                }

                String url = jsonObject.get("url").getAsString();
                if (url.trim().isEmpty()) {
                    return new Triple<>(
                        "clicked on skull with invalid url", PunishType.KICK, Collections.emptyList());
                }

                if (!(url.startsWith("http://textures.minecraft.net/texture/") || url.startsWith(
                    "https://textures.minecraft.net/texture/"))) {

                    return new Triple<>(
                        "clicked on skull with invalid url", PunishType.KICK,
                        Collections.singletonList(new Debug<>("URL", url))
                    );
                }
            }
        }
        return null;
    }
}
