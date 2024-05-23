package de.feelix.sierra.manager.storage.menu;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The MenuType enum represents the types of menus available in the game.
 *
 * <p>The menu types include generic menus, crafting menus, Anvil menu, Beacon menu, Blast Furnace menu,
 * Brewing Stand menu, Enchantment menu, Furnace menu, Grindstone menu, Hopper menu, Lectern menu,
 * Loom menu, Merchant menu, Shulker Box menu, Smithing menu, Smoker menu, Cartography Table menu,
 * Stonecutter menu, and an unknown menu type.</p>
 *
 * <p>This enum provides a {@code getMenuType} method that can be used to retrieve the MenuType
 * based on the given menu ID. It also contains a private field {@code id} to store the ID of each menu type.</p>
 *
 * @see MenuType#getMenuType(int)
 */
@RequiredArgsConstructor
@Getter
public enum MenuType {
    GENERIC_9x1(0),
    GENERIC_9x2(1),
    GENERIC_9x3(2),
    GENERIC_9x4(3),
    GENERIC_9x5(4),
    GENERIC_9x6(5),
    GENERIC_3x3(6),
    CRAFTER_3x3(7), // only in versions 1.20.3 & greater
    ANVIL(8),
    BEACON(9),
    BLAST_FURNACE(10),
    BREWING_STAND(11),
    CRAFTING(12),
    ENCHANTMENT(13),
    FURNACE(14),
    GRINDSTONE(15),
    HOPPER(16),
    LECTERN(17),
    LOOM(18),
    MERCHANT(19),
    SHULKER_BOX(20),
    SMITHING(21),
    SMOKER(22),
    CARTOGRAPHY_TABLE(23),
    STONECUTTER(24),
    UNKNOWN(-1);

    /**
     * The id variable represents the ID of the menu type.
     *
     * <p>Each menu type in the game is associated with a specific ID. This variable stores the ID
     * of the menu type represented by the MenuType enum.</p>
     *
     * @see MenuType
     */
    private final int id;

    /**
     * The MAX_ID_OLD_VERSION variable represents the maximum ID of the old version of the MenuType enum.
     *
     * <p>In the MenuType enum, each menu type is associated with a specific ID. This variable stores
     * the maximum ID of the old version of the MenuType enum.</p>
     *
     * @see MenuType#getMaxIdBasedOnVersion(ServerVersion)
     */
    private static final int MAX_ID_OLD_VERSION = 23;

    /**
     * Retrieves an array of all available menu types.
     *
     * @return An array of MenuType objects representing the available menu types.
     */
    public static MenuType[] getMenuTypeValues() {
        return MenuType.values();
    }

    /**
     * Gets the menu type based on the provided ID.
     *
     * @param id The ID of the menu type.
     * @return The menu type.
     */
    public static MenuType getMenuType(int id) {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        if (isInvalidId(id, version)) return UNKNOWN;
        if (version.isOlderThan(ServerVersion.V_1_20_3) && id >= 7) id++;
        return getMenuTypeValues()[id];
    }

    /**
     * Check if the given id is valid based on the server version.
     *
     * @param id      the id to check
     * @param version the server version
     * @return true if the id is protocol, false otherwise
     */
    // Check if id is valid
    private static boolean isInvalidId(int id, ServerVersion version) {
        return id < 0 || id > getMaxIdBasedOnVersion(version) || id >= getMenuTypeValues().length;
    }

    /**
     * This method returns the maximum ID based on the given ServerVersion.
     *
     * @param version the ServerVersion to compare against
     * @return the maximum ID based on the ServerVersion
     */
    // Get Maximum ID based on ServerVersion
    private static int getMaxIdBasedOnVersion(ServerVersion version) {
        return version.isOlderThan(ServerVersion.V_1_20_3) ? MAX_ID_OLD_VERSION : getMenuTypeValues().length - 1;
    }
}
