package de.feelix.sierra.listener.bukkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.utilities.BlockEntry;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class represents a listener for handling BlockRedstoneEvents. It detects blocks and adjusts their redstone value if necessary.
 */
public class BlockRedstoneListener implements Listener {

    /**
     * This class represents a listener for handling BlockRedstoneEvents.
     * It detects blocks and adjusts their redstone value if necessary.
     */
    private final Cache<Block, BlockEntry> cache = CacheBuilder.newBuilder()
        .expireAfterWrite(1L, TimeUnit.SECONDS)
        .build();

    /**
     * This class represents a listener for handling BlockRedstoneEvents. It detects blocks and adjusts their redstone value if necessary.
     */
    private final int limit = Sierra.getPlugin().getConfig().getInt("redstone-tick-limit", 60);

    /**
     * Handles a BlockRedstoneEvent by detecting the block and adjusting its value if necessary.
     *
     * @param event The BlockRedstoneEvent to handle.
     */
    @EventHandler
    public void handle(BlockRedstoneEvent event) {
        Block      block    = event.getBlock();
        BlockEntry detected = detectBlock(block);

        if (detected.intValue() > this.limit) {
            event.setNewCurrent(0);
            warnAndAdjustValue(detected, block);
        }
    }

    /**
     * Detects a block and updates its entry in the cache.
     *
     * @param block The block to be detected.
     * @return The detected BlockEntry object.
     */
    private BlockEntry detectBlock(Block block) {
        BlockEntry detected = this.cache.getIfPresent(block);
        if (detected == null) {
            detected = new BlockEntry(1);
            this.cache.put(block, detected);
        } else {
            detected.add(1);
        }
        return detected;
    }

    /**
     * Warns and adjusts the value of a block entry.
     *
     * @param detected The detected block entry.
     * @param block    The block associated with the detected entry.
     */
    private void warnAndAdjustValue(BlockEntry detected, Block block) {
        if (detected.intValue() == this.limit + 1) {
            Logger logger = Sierra.getPlugin().getLogger();
            logger.warning("Prevented redstone loop");
            logBlockLocation(block, logger);
            detected.setValue(this.limit + 2);
        }
    }

    /**
     * Logs the location of a block using the provided logger.
     *
     * @param block  The block whose location will be logged.
     * @param logger The logger used to log the block location.
     */
    private void logBlockLocation(Block block, Logger logger) {
        String   format   = "At: %.2f, %.2f, %.2f";
        Location location = block.getLocation();
        logger.warning(String.format(format, location.getX(), location.getY(), location.getZ()));
    }
}
