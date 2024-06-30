package de.feelix.sierra.manager.storage;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.history.HistoryDocument;
import de.feelix.sierra.utilities.update.UpdateChecker;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.events.impl.AsyncHistoryCreateEvent;
import de.feelix.sierraapi.history.History;
import de.feelix.sierraapi.history.HistoryType;
import de.feelix.sierraapi.violation.MitigationStrategy;
import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import de.feelix.sierraapi.user.UserRepository;
import de.feelix.sierraapi.user.impl.SierraUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The SierraDataManager class represents a singleton instance that manages player data in the application.
 * It provides methods to manipulate and retrieve player data from the underlying data structures.
 */
@Getter
public class SierraDataManager implements UserRepository {

    /**
     * DataManager class represents a singleton instance that manages player data in the application.
     * It provides methods to manipulate and retrieve player data from the underlying data structures.
     */
    @Getter
    private static SierraDataManager instance;

    /**
     * This variable represents a map of User objects to PlayerData objects.
     */
    private final Map<User, PlayerData> playerData = new ConcurrentHashMap<>();

    /**
     * ArrayList to store the history documents.
     */
    private final ArrayList<History> histories = new ArrayList<>();

    /**
     * This variable represents a mapping between Bukkit GameModes and custom GameModes.
     * It is used to convert between the two representations when necessary.
     */
    private static final Map<org.bukkit.GameMode, GameMode> GAMEMODE_MAP = createGameModeMap();

    /**
     * violationCount is a static variable of type HashMap<String, Integer>.
     * It represents a map that stores the count of violations for different types of checks.
     * The keys in the map are the types of checks, represented as strings.
     * The values in the map are the count of violations for each check type, represented as integers.
     * <p>
     * Example usage:
     * <p>
     * // Create a new instance of a HashMap
     * HashMap<String, Integer> violationCount = new HashMap<>();
     * <p>
     * // Add a violation count for a check type
     * violationCount.put("Check Type 1", 5);
     * <p>
     * // Increment the violation count for a check type
     * violationCount.put("Check Type 2", violationCount.getOrDefault("Check Type 2", 0) + 1);
     * <p>
     * // Get the violation count for a check type
     * int count = violationCount.getOrDefault("Check Type 1", 0);
     * <p>
     * // Remove a check type and its violation count
     * violationCount.remove("Check Type 2");
     */
    public static HashMap<String, Integer> violationCount = new HashMap<>();

    /**
     * The KICKS variable represents the number of kicks in this session
     * <p>
     * Note: KICKS is a static variable which means it is shared across all instances
     * of the containing class SierraDataManager.
     */
    public static int KICKS = 0;

    /**
     * The BANS variable represents the number of bans in this session
     * <p>
     * Note: BANS is a static variable which means it is shared across all instances
     * of the containing class SierraDataManager.
     */
    public static int BANS = 0;

    /**
     * Determines whether to skip the Skull UUID check.
     */
    public static boolean skipSkullUUIDCheck = false;

    /**
     * Determines whether to skip model check or not.
     */
    public static boolean skipModelCheck = false;

    /**
     * Determines whether to skip the anvil check.
     * <p>
     * If set to true, the anvil check will be skipped.
     * If set to false, the anvil check will be performed.
     */
    public static boolean skipAnvilCheck = false;

    /**
     * The DataManager function initializes the packet priority.
     */
    public SierraDataManager() {
        instance = this;

        this.initializePacketListeners();
    }

    /**
     * The initializePacketListeners function is used to register a PacketListenerCommon object with the
     * PacketEvents API. This listener will be called whenever a player connects or disconnects from the server,
     * and we can use this to add/remove PlayerData objects for each player.
     */
    private void initializePacketListeners() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerCommon() {

            @Override
            public void onUserConnect(UserConnectEvent event) {
                User user = event.getUser();
                addPlayerData(user);
                checkIfBlocked(user);
                checkForUpdate(user);
            }

            @Override
            public void onUserDisconnect(UserDisconnectEvent event) {
                removePlayerData(event.getUser());
            }
        });
    }

    /**
     * Checks if the connection of a user should be blocked based on the ban configuration.
     *
     * @param user The user whose connection needs to be checked.
     */
    private void checkIfBlocked(User user) {
        if (Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("block-connections-after-ban", true)) {
            String hostAddress = user.getAddress().getAddress().getHostAddress();
            if (Sierra.getPlugin().getAddressStorage().invalid(hostAddress)) {
                Sierra.getPlugin()
                    .getLogger()
                    .info("Connection of " + hostAddress + " got blocked, cause it was punished recently");
                PlayerData data = Sierra.getPlugin().getSierraDataManager().getPlayerData(user).get();

                if (data == null) return;

                data.punish(MitigationStrategy.KICK);
            }
        }
    }

    /**
     * Checks for any updates and sends a message to the player if their version is outdated and they can update.
     *
     * @param user The user to check for update.
     */
    private void checkForUpdate(User user) {
        FoliaScheduler.getAsyncScheduler().runNow(Sierra.getPlugin(), o -> {

            // Sleep task for 1 second
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (!isVersionOutdated() || !isUserValid(user) || isVersionInvalid()) return;

            Player player = getPlayer(user);

            if (player == null || !playerCanUpdate(player)) return;

            sendMessage(user);
        });
    }

    /**
     * Checks if the version of the plugin Sierra is protocol.
     *
     * @return true if the version is protocol, false otherwise
     */
    private boolean isVersionInvalid() {
        return Sierra.getPlugin()
            .getUpdateChecker()
            .getLatestReleaseVersion()
            .equalsIgnoreCase(UpdateChecker.UNKNOWN_VERSION);
    }

    /**
     * Checks if the current version of the plugin is outdated compared to the latest release version.
     *
     * @return true if the current version is outdated, false otherwise
     */
    private boolean isVersionOutdated() {
        String localVersion         = Sierra.getPlugin().getDescription().getVersion();
        String latestReleaseVersion = Sierra.getPlugin().getUpdateChecker().getLatestReleaseVersion();

        return !latestReleaseVersion.equalsIgnoreCase(localVersion);
    }

    /**
     * Checks if a User is valid.
     *
     * @param user the User to check
     * @return true if the User is valid, false otherwise
     */
    private boolean isUserValid(User user) {
        return user != null && user.getName() != null;
    }

    /**
     * Retrieves the Player object associated with the given User.
     *
     * @param user the User to retrieve the Player for
     * @return the Player object associated with the User, or null if not found
     */
    private Player getPlayer(User user) {
        return Bukkit.getPlayer(user.getName());
    }

    /**
     * Checks if the player can update.
     *
     * @param player the player to check
     * @return true if the player can update, false otherwise
     */
    private boolean playerCanUpdate(Player player) {
        return player.hasPermission("sierra.update") || player.isOp();
    }

    /**
     * Adds a kick to the violation count for the specified check type.
     * <p>
     * If the check type is not already in the violation count map, a new entry is added with a count of 1.
     * If the check type is already in the violation count map, the count is incremented by 1.
     *
     * @param checkType the type of check to add a kick for
     */
    public void addKick(CheckType checkType) {
        String name = checkType.getFriendlyName().replace(" ", "");
        if (!violationCount.containsKey(name)) {
            violationCount.put(name, 1);
            return;
        }
        violationCount.put(name, violationCount.get(name) + 1);
    }

    /**
     * Sends a message to the specified user.
     *
     * @param user The user to send the message to.
     */
    private void sendMessage(User user) {
        String localVersion         = Sierra.getPlugin().getDescription().getVersion();
        String latestReleaseVersion = Sierra.getPlugin().getUpdateChecker().getLatestReleaseVersion();

        user.sendMessage(Sierra.PREFIX + " §cServer is running an outdated version of Sierra");
        user.sendMessage(Sierra.PREFIX + " §fLocal: §c" + localVersion + "§f, Latest: §a" + latestReleaseVersion);
        user.sendMessage(
            LegacyComponentSerializer.legacy('&')
                .deserialize(Sierra.PREFIX + " &c&nToo lazy? Stay always up-to-date with SierraLoader&c")
                .clickEvent(ClickEvent.clickEvent(
                    ClickEvent.Action.OPEN_URL,
                    "https://sierra.squarecode.de/sierra/sierra-documentation/sierra-loader"
                ))
                .hoverEvent(HoverEvent.showText(Component.text("§7Click me to view the documentation"))));
    }

    /**
     * Creates a punishment history entry for a user.
     *
     * @param username   The username of the user to create the punishment history for.
     * @param mitigationStrategy The type of punishment applied.
     */
    public void createPunishmentHistory(String username, String clientVersion, MitigationStrategy mitigationStrategy, long ping,
                                        String description) {

        throwHistory(new HistoryDocument(username, description, clientVersion,
                                         ping, mitigationStrategy, HistoryType.PUNISH
        ));
    }

    /**
     * Creates a mitigation history entry for a user.
     * <p>
     * This method creates a HistoryDocument object for a mitigation history entry and throws it using the
     * throwHistory()
     * method to add it to the collection of histories.
     *
     * @param username    The username of the user to create the mitigation history for.
     * @param mitigationStrategy  The type of punishment applied.
     * @param ping        The user's ping at the time of the mitigation.
     * @param description The description of the mitigation.
     * @see HistoryDocument
     * @see MitigationStrategy
     * @see HistoryType
     */
    public void createMitigateHistory(String username, String clientVersion, MitigationStrategy mitigationStrategy, long ping,
                                      String description) {
        throwHistory(new HistoryDocument(
            username, description, clientVersion, ping, mitigationStrategy, HistoryType.MITIGATE));
    }

    /**
     * Throws a history event and adds the history document to the collection of histories.
     *
     * @param document The history document to be thrown and added.
     */
    private void throwHistory(HistoryDocument document) {
        FoliaScheduler.getAsyncScheduler().runNow(
            Sierra.getPlugin(),
            o -> Sierra.getPlugin().getEventBus().publish(new AsyncHistoryCreateEvent(document))
        );

        this.histories.add(document);
    }

    /**
     * The getPlayerData function is used to get the PlayerData object associated with a given User.
     *
     * @param user user Get the player data for a specific user
     * @return A weak reference to the player data object
     */
    public WeakReference<PlayerData> getPlayerData(User user) {
        return new WeakReference<>(this.playerData.get(user));
    }

    /**
     * The addPlayerData function adds a new PlayerData object to the playerData HashMap.
     *
     * @param user user Get the player's data
     */
    public void addPlayerData(User user) {
        PlayerData value = new PlayerData(user);
        this.playerData.put(user, value);
    }

    /**
     * Sets the game mode for a player.
     *
     * @param value  The PlayerData object associated with the player.
     * @param player The player object to set the game mode for.
     */
    public void setPlayerGameMode(PlayerData value, Object player) {
        org.bukkit.GameMode gameMode          = ((Player) player).getGameMode();
        GameMode            correspondingMode = GAMEMODE_MAP.get(gameMode);
        if (correspondingMode != null) {
            value.setGameMode(correspondingMode);
        }
    }

    /**
     * The removePlayerData function removes the player data from the HashMap.
     *
     * @param user user Remove the player data of a specific user
     */
    public void removePlayerData(User user) {
        this.playerData.remove(user);
    }

    /**
     * Creates a map that maps Bukkit GameMode objects to custom GameMode objects.
     *
     * @return A map containing the mapping of Bukkit GameMode objects to custom GameMode objects.
     */
    private static Map<org.bukkit.GameMode, GameMode> createGameModeMap() {
        Map<org.bukkit.GameMode, GameMode> gameModeMap = new HashMap<>();
        gameModeMap.put(org.bukkit.GameMode.SURVIVAL, GameMode.SURVIVAL);
        gameModeMap.put(org.bukkit.GameMode.CREATIVE, GameMode.CREATIVE);
        gameModeMap.put(org.bukkit.GameMode.ADVENTURE, GameMode.ADVENTURE);
        gameModeMap.put(org.bukkit.GameMode.SPECTATOR, GameMode.SPECTATOR);
        return gameModeMap;
    }

    @Override
    public Optional<SierraUser> queryUserByUuid(UUID uuid) {
        for (User user : playerData.keySet()) {
            if (user.getUUID() == uuid) {
                return Optional.of(playerData.get(user));
            }
        }
        return Optional.empty();
    }

    /**
     * Queries for a SierraUser by the given entityId.
     *
     * @param id The entityId to query for
     * @return An Optional containing the SierraUser object if found, otherwise an empty Optional
     */
    @Override
    public Optional<SierraUser> queryUserByEntityId(int id) {
        for (User user : playerData.keySet()) {
            if (user.getEntityId() == id) {
                return Optional.of(playerData.get(user));
            }
        }
        return Optional.empty();
    }

    /**
     * The queryUserByName method is used to query for a SierraUser object
     * by the given name. It searches for a matching user in the playerData HashMap
     * and returns the corresponding SierraUser object if found.
     *
     * @param name The name to search for in the playerData HashMap
     * @return Optional containing the SierraUser object if found, otherwise an empty Optional
     */
    @Override
    public Optional<SierraUser> queryUserByName(String name) {
        for (User user : playerData.keySet()) {
            if (user.getName().equalsIgnoreCase(name)) {
                return Optional.of(playerData.get(user));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<History> getLocalActionHistory() {
        return this.histories;
    }
}
