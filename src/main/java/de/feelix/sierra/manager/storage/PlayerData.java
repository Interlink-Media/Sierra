package de.feelix.sierra.manager.storage;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.Location;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.CheckManager;
import de.feelix.sierra.manager.storage.alert.AbstractAlertSetting;
import de.feelix.sierra.manager.storage.logger.SierraLogger;
import de.feelix.sierra.manager.storage.processor.*;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierra.utilities.message.ConfigValue;
import de.feelix.sierraapi.timing.TimingHandler;
import de.feelix.sierraapi.user.settings.AlertSettings;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Data;
import de.feelix.sierraapi.check.CheckRepository;
import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.MitigationStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * PlayerData is a class representing the data associated with a player.
 */
@Data
public class PlayerData implements SierraUser {

    private Object        player;
    private User          user;
    private GameMode      gameMode;
    private Location      lastLocation;
    private ClientVersion clientVersion;

    private       String      brand    = "vanilla";
    private       String      locale   = "unset";
    private final Set<String> channels = new HashSet<>();
    private final long        joinTime = System.currentTimeMillis();

    private boolean receivedPunishment = false;
    private boolean exempt             = false;
    private boolean nameChecked        = false;
    private boolean bypassPermission   = false;

    private double bytesSent = 0;

    private final AlertSettings alertSettings      = new AbstractAlertSetting();
    private final AlertSettings mitigationSettings = new AbstractAlertSetting();

    private       SierraLogger         sierraLogger;
    private final CheckManager         checkManager         = new CheckManager(this);
    private final BrandProcessor       brandProcessor       = new BrandProcessor(this);
    private final GameModeProcessor    gameModeProcessor    = new GameModeProcessor(this);
    private final PingProcessor        pingProcessor        = new PingProcessor(this);
    private final TransactionProcessor transactionProcessor = new TransactionProcessor(this);
    private final TimingHandler        timingProcessor      = new TimingProcessor(this);

    /**
     * The PlayerData function is a constructor that takes in a User object and sets the user variable to it.
     *
     * @param user user Set the user field in this class
     */
    public PlayerData(User user) {
        this.user = user;
        this.clientVersion = user.getClientVersion();
    }

    /**
     * This method polls data for the given player.
     *
     * @param bukkitPlayer the Bukkit player object for which data is to be polled
     */
    public void pollData(Player bukkitPlayer) {
        this.player = bukkitPlayer;
        bypassPermission = bukkitPlayer.hasPermission("sierra.bypass");
        if (this.sierraLogger == null) {
            sierraLogger = new SierraLogger(bukkitPlayer.getName());
        }
    }

    /**
     * Sends a transaction for the user.
     */
    public void sendTransaction() {
        this.transactionProcessor.sendTransaction();
    }

    /**
     * Retrieves the username associated with the PlayerData object.
     *
     * @return the username as a String
     */
    @Override
    public String username() {
        return user.getName();
    }

    /**
     * Retrieves the brand of the user.
     *
     * @return The brand of the user as a String.
     */
    @Override
    public String brand() {
        return brand;
    }

    @Override
    public String locale() {
        return locale;
    }

    /**
     * Retrieves the entity ID associated with the player.
     *
     * @return the entity ID as an integer
     */
    @Override
    public int entityId() {
        return user.getEntityId();
    }

    /**
     * The ping method sends a ping request and returns
     * the round trip time in milliseconds.
     *
     * @return The round trip time in milliseconds as an integer.
     */
    @Override
    public int ping() {
        return (int) this.getPingProcessor().getPing();
    }

    /**
     * Retrieves the number of ticks that the PlayerData object has existed for.
     *
     * @return The number of ticks as an integer.
     */
    @Override
    public int ticksExisted() {
        return this.getTicksExisted();
    }

    /**
     * Returns the UUID of the player.
     *
     * @return the UUID of the player
     */
    @Override
    public UUID uuid() {
        return user.getUUID();
    }

    /**
     * Retrieves the timestamp representing the existence of the object since its creation.
     *
     * @return the timestamp representing the existence of the object since its creation
     */
    @Override
    public long existSince() {
        return System.currentTimeMillis() - joinTime;
    }

    /**
     * Retrieves the version of the client used by the player.
     *
     * @return the version of the client as a String
     */
    @Override
    public String version() {
        return user.getClientVersion().name().replace("V_", "").replace("_", ".");
    }

    /**
     * The kick method is used to kick the user associated with the PlayerData object.
     * If the user is not null, it will close the user's connection and return true.
     * If the user is null, it will return false.
     *
     * @return true if the user is successfully kicked, false otherwise
     */
    @Override
    public boolean kick() {
        SierraDataManager.KICKS++;
        if (user != null) {
            user.closeConnection();
            return true;
        }
        return false;
    }

    /**
     * Returns whether the player is exempt from certain actions or checks.
     *
     * @return true if the player is exempt, false otherwise
     */
    @Override
    public boolean isExempt() {
        return this.exempt;
    }

    /**
     * Retrieves the game mode of the player.
     *
     * @return The game mode of the player as an instance of GameMode.
     */
    @Override
    public GameMode gameMode() {
        return this.getGameMode();
    }

    /**
     * Sets whether the player is exempt from certain actions or checks.
     *
     * @param b {@code true} if the player should be exempt, {@code false} otherwise
     * @return the updated value of the exempt field
     */
    @Override
    public boolean setExempt(boolean b) {
        this.exempt = b;
        return this.exempt;
    }

    /**
     * Retrieves the alert settings for the user.
     *
     * @return The alert settings for the user as an instance of {@link AlertSettings}.
     */
    @Override
    public AlertSettings alertSettings() {
        return alertSettings;
    }

    /**
     * Retrieves the mitigation settings for the user.
     *
     * @return The mitigation settings for the user as an instance of {@link AlertSettings}.
     * @see SierraUser#mitigationSettings()
     * @see AlertSettings
     */
    @Override
    public AlertSettings mitigationSettings() {
        return mitigationSettings;
    }

    /**
     * Disconnects the user associated with the PlayerData object and logs a message with the provided exception.
     *
     * @param exception the exception that occurred
     */
    public void exceptionDisconnect(Exception exception) {

        // To prevent future console spam cause thread is not fast enough
        if (receivedPunishment) return;

        Sierra.getPlugin().getLogger().warning("We disconnect " + this.username() + " for security purpose");
        Sierra.getPlugin().getLogger().warning("Exception: " + exception.getMessage());
        punish(MitigationStrategy.KICK);
    }

    /**
     * Executes a punishment action based on the given punish type.
     * If the punish type is BAN and the ban feature is enabled, the player will be banned.
     * In any case, the player will be kicked.
     *
     * @param mitigationStrategy the type of punishment to be applied
     */
    public void punish(MitigationStrategy mitigationStrategy) {
        setReceivedPunishment(true);
        if (mitigationStrategy == MitigationStrategy.BAN && Sierra.getPlugin().getPunishmentConfig().isBan()) {
            ban();
        }
        kick();
    }

    /**
     * The ban method is used to ban a player.
     * It retrieves the punish command from the sierra.yml configuration file and replaces the {username} placeholder
     * with the player's name.
     * Then, it dispatches the command to the console sender.
     */
    private void ban() {
        SierraDataManager.BANS++;
        FoliaScheduler.getGlobalRegionScheduler().run(Sierra.getPlugin(), o -> Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            new ConfigValue("punish-command", "ban {username} Crashing", false)
                .replace("{username}", this.user.getName())
                .message()
        ));
    }

    /**
     * Retrieves the number of ticks that the PlayerData object has existed for.
     *
     * @return The number of ticks as an integer.
     */
    public int getTicksExisted() {
        return FormatUtils.convertMillisToTicks(System.currentTimeMillis() - this.joinTime);
    }

    /**
     * Retrieves the check repository associated with this instance.
     *
     * @return The check repository associated with this instance.
     */
    @Override
    public CheckRepository checkRepository() {
        return this.checkManager;
    }

    /**
     * Retrieves the TimingHandler object associated with the PlayerData object.
     *
     * @return the TimingHandler object
     * @see SierraUser#timingHandler()
     */
    @Override
    public TimingHandler timingHandler() {
        return this.getTimingProcessor();
    }

    /**
     * Retrieves the bypass permission status of the PlayerData object.
     *
     * @return true if the player has bypass permission, false otherwise
     */
    public boolean hasBypassPermission() {
        return this.bypassPermission;
    }
}
