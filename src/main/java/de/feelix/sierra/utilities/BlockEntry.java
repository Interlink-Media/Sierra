package de.feelix.sierra.utilities;

/**
 * The BlockEntry class represents an entry for a block in the cache.
 * It contains a single integer value that can be modified and retrieved.
 */
public class BlockEntry {

    /**
     * This private variable represents an integer value.
     */
    private int integer;

    /**
     * The `BlockEntry` class represents an entry for a block in the cache.
     * It contains a single integer value that can be modified and retrieved.
     */
    public BlockEntry(int integer) {
        this.integer = integer;
    }

    /**
     * Retrieves the integer value from the BlockEntry object.
     *
     * @return The integer value of the BlockEntry.
     */
    public int intValue() {
        return this.integer;
    }

    /**
     * Sets the integer value of the BlockEntry object.
     *
     * @param integer The integer value to set.
     */
    public void setValue(int integer) {
        this.integer = integer;
    }

    /**
     * Adds the provided integer value to the current integer value of the BlockEntry object.
     *
     * @param integer The integer value to add.
     * @return The updated BlockEntry object.
     */
    public BlockEntry add(int integer) {
        this.integer += integer;
        return this;
    }
}
