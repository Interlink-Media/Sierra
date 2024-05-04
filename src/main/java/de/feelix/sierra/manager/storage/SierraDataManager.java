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
import de.feelix.sierraapi.events.impl.AsyncHistoryCreateEventSierra;
import de.feelix.sierraapi.history.History;
import de.feelix.sierraapi.history.HistoryType;
import de.feelix.sierraapi.violation.PunishType;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;
import lombok.Getter;
import de.feelix.sierraapi.user.UserRepository;
import de.feelix.sierraapi.user.impl.SierraUser;
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
    private static SierraDataManager     instance;

    /**
     * This variable represents a map of User objects to PlayerData objects.
     */
    private final  Map<User, PlayerData> playerData = new ConcurrentHashMap<>();

    /**
     * ArrayList to store the history documents.
     */
    private final ArrayList<History> histories = new ArrayList<>();

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
                addPlayerData(event.getUser());
                checkForUpdate(event.getUser());
            }

            @Override
            public void onUserDisconnect(UserDisconnectEvent event) {
                removePlayerData(event.getUser());
            }
        });
    }

    /**
     * Checks for any updates and sends a message to the player if their version is outdated and they can update.
     *
     * @param user The user to check for update.
     */
    private void checkForUpdate(User user) {
        FoliaCompatUtil.runTaskAsync(Sierra.getPlugin(), () -> {

            // Sleep task for 1 second
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (!isVersionOutdated() || !isUserValid(user) || isVersionInvalid()) return;

            Player player = getPlayer(user);
            if (player == null || !playerCanUpdate(player)) return;

            sendMessage(player);
        });
    }

    /**
     * Checks if the version of the plugin Sierra is invalid.
     *
     * @return true if the version is invalid, false otherwise
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
     * Sends a message to the specified player to inform them that the current version
     * of the Sierra plugin is outdated.
     *
     * @param player The Player to send the message to.
     */
    private void sendMessage(Player player) {
        String localVersion         = Sierra.getPlugin().getDescription().getVersion();
        String latestReleaseVersion = Sierra.getPlugin().getUpdateChecker().getLatestReleaseVersion();

        player.sendMessage(Sierra.PREFIX + " §cThis version of Sierra is outdated!");
        player.sendMessage(Sierra.PREFIX + " §fLocal: §c" + localVersion + "§f, Latest: §a" + latestReleaseVersion);
    }

    /**
     * Creates a punishment history entry for a user.
     *
     * @param username   The username of the user to create the punishment history for.
     * @param punishType The type of punishment applied.
     */
    public void createPunishmentHistory(String username, PunishType punishType, long ping, String description) {

        HistoryDocument document = new HistoryDocument(
            username, description, ping, punishType, HistoryType.PUNISH);

        throwHistory(document);
    }

    /**
     * Throws a history event and adds the history document to the collection of histories.
     *
     * @param document The history document to be thrown and added.
     */
    private void throwHistory(HistoryDocument document) {
        FoliaCompatUtil.runTaskAsync(
            Sierra.getPlugin(),
            () -> Sierra.getPlugin().getEventBus().publish(new AsyncHistoryCreateEventSierra(document))
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
        value.setGameMode(GameMode.defaultGameMode());
        this.playerData.put(user, value);
    }

    /**
     * The removePlayerData function removes the player data from the HashMap.
     *
     * @param user user Remove the player data of a specific user
     */
    public void removePlayerData(User user) {
        this.playerData.remove(user);
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
