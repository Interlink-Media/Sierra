package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.violation.PunishType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

//Fixes client-side crash books

/**
 * The CreativeClientBookCrash class represents a book that can crash the client when opened.
 * It is responsible for handling the check for an item and determining if it contains protocol translation keys.
 */
//A book with the nbt from below will crash a client when opened
//{generation:0,pages:[0:"{translate:translation.test.protocol}",],author:"someone",title:"a",resolved:1b,}
//{generation:0,pages:[0:"{translate:translation.test.invalid2}",],author:"someone",title:"a",resolved:1b,}
public class CreativeClientBookCrash implements ItemCheck {

    private static final Pattern PATTERN = Pattern.compile("\\s");

    @Override
    public Triple<String, PunishType, List<Debug<?>>> handleCheck(PacketReceiveEvent event, ItemStack clickedStack,
                                                                  NBTCompound nbtCompound, PlayerData playerData) {
        List<String> pages = getPages(nbtCompound);
        if (pages.isEmpty()) {
            return null;
        }
        for (String page : pages) {
            String withOutSpaces = PATTERN.matcher(page).replaceAll("");
            if (withOutSpaces.toLowerCase().contains("{translate:translation.test.protocol}") || withOutSpaces.contains(
                "{translate:translation.test.invalid2}")) {
                return new Triple<>("interacted with an invalid translation", PunishType.KICK, Collections.emptyList());
            }
        }
        return null;
    }

    /**
     * Returns a list of pages from the given NBT compound.
     *
     * @param nbtCompound the NBT compound to retrieve pages from
     * @return a List of String representing the pages, or an empty List if there are no pages
     */
    private List<String> getPages(NBTCompound nbtCompound) {
        List<String>       pageList = new ArrayList<>();
        NBTList<NBTString> nbtList  = nbtCompound.getStringListTagOrNull("pages");
        if (nbtList != null) {
            for (NBTString tag : nbtList.getTags()) {
                pageList.add(tag.getValue());
            }
        }
        return pageList;
    }
}
