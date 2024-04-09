package de.feelix.sierra.manager.storage;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import lombok.Getter;
import de.feelix.sierraapi.user.UserRepository;
import de.feelix.sierraapi.user.impl.SierraUser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class DataManager implements UserRepository {

    @Getter
    private static DataManager           instance;
    private final  Map<User, PlayerData> playerData = new ConcurrentHashMap<>();

    private final ArrayList<HistoryDocument> histories = new ArrayList<>();

    /**
     * The DataManager function initializes the packet listeners.
     */
    public DataManager() {
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
            }

            @Override
            public void onUserDisconnect(UserDisconnectEvent event) {
                removePlayerData(event.getUser());
            }
        });
    }

    /**
     * The getPlayerData function is used to get the PlayerData object associated with a given User.
     *
     * @param user user Get the player data for a specific user
     * @return A weakreference to the playerdata object
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

    @Override
    public Optional<SierraUser> queryUserByEntityId(int id) {
        for (User user : playerData.keySet()) {
            if (user.getEntityId() == id) {
                return Optional.of(playerData.get(user));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SierraUser> queryUserByName(String name) {
        for (User user : playerData.keySet()) {
            if (user.getName().equalsIgnoreCase(name)) {
                return Optional.of(playerData.get(user));
            }
        }
        return Optional.empty();
    }
}
