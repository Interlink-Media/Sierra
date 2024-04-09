package de.feelix.sierra.check.impl.creative.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.feelix.sierra.check.impl.creative.ItemCheck;
import de.feelix.sierra.utilities.CrashDetails;
import de.feelix.sierraapi.violation.PunishType;

public class InvalidPlainNbt implements ItemCheck {

    private CrashDetails invalidNbt(ItemStack itemStack) {

        NBTCompound tag = itemStack.getNBT();

        if (tag != null) {
            CrashDetails crashDetails = checkForSpawner(tag);
            if (crashDetails != null) {
                return crashDetails;
            }
            return checkValidMap(tag);
        }
        return null;
    }

    private CrashDetails checkValidMap(NBTCompound nbtTag) {
        NBTNumber range       = nbtTag.getNumberTagOrNull("range");
        int       maxMapRange = 15;
        if (range != null && (range.getAsInt() > maxMapRange || range.getAsInt() < 0)) {
            return new CrashDetails("Range tag at " + range.getAsInt(), PunishType.BAN);
        }
        return null;
    }

    private CrashDetails checkForSpawner(NBTCompound tag) {

        NBTNumber spawnRange          = tag.getNumberTagOrNull("SpawnRange");
        NBTNumber requiredPlayerRange = tag.getNumberTagOrNull("RequiredPlayerRange");
        NBTNumber maxNearbyEntities   = tag.getNumberTagOrNull("MaxNearbyEntities");

        if (maxNearbyEntities != null && (maxNearbyEntities.getAsInt() > Byte.MAX_VALUE ||
                                          maxNearbyEntities.getAsInt() < 0)) {
            return new CrashDetails("MaxNearbyEntities at: " + maxNearbyEntities.getAsInt(), PunishType.BAN);
        }

        NBTNumber spawnCount = tag.getNumberTagOrNull("SpawnCount");
        NBTNumber delay      = tag.getNumberTagOrNull("Delay");

        if (delay != null && (delay.getAsInt() > Short.MAX_VALUE || delay.getAsInt() < 0)) {
            return new CrashDetails("Delay at: " + delay.getAsInt(), PunishType.BAN);
        }

        NBTNumber maxSpawnDelay = tag.getNumberTagOrNull("MaxSpawnDelay");
        NBTNumber minSpawnDelay = tag.getNumberTagOrNull("MinSpawnDelay");

        if (minSpawnDelay != null && (minSpawnDelay.getAsInt() > Short.MAX_VALUE ||
                                      minSpawnDelay.getAsInt() < 0)) {
            return new CrashDetails("MinSpawnDelay at: " + minSpawnDelay.getAsInt(), PunishType.BAN);
        }

        int maxAllowedSpawnRange          = 20;
        int maxAllowedSpawnDelay          = 1000;
        int maxAllowedSpawnCount          = 30;
        int maxAllowedRequiredPlayerRange = 16;

        if (spawnRange != null && (spawnRange.getAsInt() > maxAllowedSpawnRange || spawnRange.getAsInt() < 0))
            return new CrashDetails("SpawnRange at: " + spawnRange.getAsInt(), PunishType.BAN);

        if (maxSpawnDelay != null && (maxSpawnDelay.getAsInt() > maxAllowedSpawnDelay ||
                                      maxSpawnDelay.getAsInt() < 0)) {
            return new CrashDetails("MaxSpawnDelay at: " + maxSpawnDelay.getAsInt(), PunishType.BAN);
        }

        if (spawnCount != null && (spawnCount.getAsInt() > maxAllowedSpawnCount || spawnCount.getAsInt() < 0))
            return new CrashDetails("SpawnCount at: " + spawnCount.getAsInt(), PunishType.BAN);

        if (requiredPlayerRange != null && (requiredPlayerRange.getAsInt() > maxAllowedRequiredPlayerRange ||
                                            requiredPlayerRange.getAsInt() < 0)) {
            new CrashDetails("RequiredPlayerRange at: " + requiredPlayerRange.getAsInt(), PunishType.BAN);
        }
        return null;
    }

    @Override
    public CrashDetails handleCheck(PacketReceiveEvent event, ItemStack clickedStack, NBTCompound nbtCompound) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
            if (wrapper.getItemStack().isPresent()) {
                return invalidNbt(wrapper.getItemStack().get());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper      = new WrapperPlayClientClickWindow(event);
            return invalidNbt(wrapper.getCarriedItemStack());
        }
        return null;
    }
}
