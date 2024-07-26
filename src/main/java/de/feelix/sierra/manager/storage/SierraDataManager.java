package de.feelix.sierra.manager.storage;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
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

    private static final String VERSION_START_TAG = "\"tag_name\":\"";
    private static final String GITHUB_API_BASE_URL = "https://api.github.com/repos/";
    private static final String GITHUB_API_RELEASES = "/releases/latest";

    public static final String UNKNOWN_VERSION = "UNKNOWN";
    public static final Map<String, Integer> violationCount = new HashMap<>();
    public static int KICKS = 0;
    public static int BANS = 0;
    public static boolean skipSkullUUIDCheck = false;
    public static boolean skipModelCheck = false;
    public static boolean skipAnvilCheck = false;

    @Getter
    private static SierraDataManager instance;
    private final Map<User, PlayerData> playerData = new ConcurrentHashMap<>();
    private final List<History> histories = new ArrayList<>();

    public SierraDataManager() {
        instance = this;
        initializePacketListeners();
    }

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

    private void checkIfBlocked(User user) {
        if (Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("block-connections-after-ban", true)) {
            String hostAddress = user.getAddress().getAddress().getHostAddress();
            if (Sierra.getPlugin().getAddressStorage().invalid(hostAddress)) {
                Sierra.getPlugin()
                    .getLogger()
                    .info("Connection of " + hostAddress + " got blocked, cause it was punished recently");

                PlayerData data = getPlayerData(user).get();

                if (data != null) {
                    data.punish(MitigationStrategy.KICK);
                }
            }
        }
    }

    private void checkForUpdate(User user) {
        FoliaScheduler.getAsyncScheduler().runNow(Sierra.getPlugin(), o -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            if (isVersionOutdated() && isUserValid(user) && !isVersionInvalid()) {
                Player player = getPlayer(user);
                if (player != null && playerCanUpdate(player)) {
                    sendMessage(user);
                }
            }
        });
    }

    private boolean isVersionInvalid() {
        return Sierra.getPlugin()
            .getUpdateChecker()
            .getLatestReleaseVersion()
            .equalsIgnoreCase(UpdateChecker.UNKNOWN_VERSION);
    }

    private boolean isVersionOutdated() {
        String localVersion = Sierra.getPlugin().getDescription().getVersion();
        String latestReleaseVersion = Sierra.getPlugin().getUpdateChecker().getLatestReleaseVersion();
        return !latestReleaseVersion.equalsIgnoreCase(localVersion);
    }

    private boolean isUserValid(User user) {
        return user != null && user.getName() != null;
    }

    private Player getPlayer(User user) {
        return Bukkit.getPlayer(user.getName());
    }

    private boolean playerCanUpdate(Player player) {
        return player.hasPermission("sierra.update") || player.isOp();
    }

    public void addKick(CheckType checkType) {
        String name = checkType.getFriendlyName().replace(" ", "");
        violationCount.merge(name, 1, Integer::sum);
    }

    private void sendMessage(User user) {
        String localVersion = Sierra.getPlugin().getDescription().getVersion();
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
                .hoverEvent(HoverEvent.showText(Component.text("§7Click me to view the documentation")))
        );
    }

    public void createPunishmentHistory(String username, String clientVersion, MitigationStrategy mitigationStrategy,
                                        long ping, String description) {
        createHistory(username, clientVersion, mitigationStrategy, ping, description, HistoryType.PUNISH);
    }

    public void createMitigateHistory(String username, String clientVersion, MitigationStrategy mitigationStrategy,
                                      long ping, String description) {
        createHistory(username, clientVersion, mitigationStrategy, ping, description, HistoryType.MITIGATE);
    }

    private void createHistory(String username, String clientVersion, MitigationStrategy mitigationStrategy, long ping,
                               String description, HistoryType type) {

        FoliaScheduler.getAsyncScheduler()
            .runNow(Sierra.getPlugin(), o -> {

                        HistoryDocument document = new HistoryDocument(
                            username, description, clientVersion, ping, mitigationStrategy, type);

                        AsyncHistoryCreateEvent event = new AsyncHistoryCreateEvent(document);
                        Sierra.getPlugin().getEventBus().publish(event);

                        if (!event.isCancelled()) {
                            histories.add(document);
                        }
                    }
            );
    }

    public WeakReference<PlayerData> getPlayerData(User user) {
        return new WeakReference<>(playerData.get(user));
    }

    public void addPlayerData(User user) {
        playerData.put(user, new PlayerData(user));
    }

    public void removePlayerData(User user) {
        PlayerData data = playerData.get(user);

        if (data != null && data.getSierraLogger() != null) {
            data.getSierraLogger().close();
        }
        playerData.remove(user);
    }

    public static void increaseKickValue() {
        SierraDataManager.KICKS++;
    }

    public static void increaseBanValue() {
        SierraDataManager.BANS++;
    }

    @Override
    public Optional<SierraUser> queryUserByUuid(UUID uuid) {
        return playerData.keySet().stream()
            .filter(user -> user.getUUID().equals(uuid))
            .map(user -> (SierraUser) playerData.get(user))
            .findFirst();
    }

    @Override
    public Optional<SierraUser> queryUserByEntityId(int id) {
        return playerData.keySet().stream()
            .filter(user -> user.getEntityId() == id)
            .map(user -> (SierraUser) playerData.get(user))
            .findFirst();
    }

    @Override
    public Optional<SierraUser> queryUserByName(String name) {
        return playerData.keySet().stream()
            .filter(user -> user.getName().equalsIgnoreCase(name))
            .map(user -> (SierraUser) playerData.get(user))
            .findFirst();
    }

    @Override
    public List<History> getLocalActionHistory() {
        return histories;
    }
}
