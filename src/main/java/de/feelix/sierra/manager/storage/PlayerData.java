package de.feelix.sierra.manager.storage;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.CheckManager;
import de.feelix.sierra.manager.storage.alert.AbstractAlertSetting;
import de.feelix.sierra.manager.storage.processor.*;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.timing.TimingHandler;
import de.feelix.sierraapi.user.settings.AlertSettings;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Data;
import de.feelix.sierraapi.check.CheckRepository;
import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * PlayerData is a class representing the data associated with a player.
 */
@Data
public class PlayerData implements SierraUser {

    private       User        user;
    private final Set<String> channels = new HashSet<>();

    private int lastBookEditTick;
    private int lastDropItemTick;
    private int lastCraftRequestTick;
    private int dropCount;
    private int recursionCount;
    private int bytesSent;
    private int openWindowType;
    private int openWindowContainer;

    private double packetCount;
    private double packetAllowance = 1000;

    private long joinTime = System.currentTimeMillis();

    private boolean receivedPunishment = false;
    private boolean exempt             = false;
    private boolean hasBrand           = false;
    private boolean nameChecked        = false;
    private boolean hasOpenAnvil       = false;

    private String brand = "vanilla";

    private ClientVersion clientVersion;
    private GameMode      gameMode;

    private final AlertSettings alertSettings      = new AbstractAlertSetting();
    private final AlertSettings mitigationSettings = new AbstractAlertSetting();

    private final CheckManager      checkManager      = new CheckManager(this);
    private final BrandProcessor    brandProcessor    = new BrandProcessor(this);
    private final GameModeProcessor gameModeProcessor = new GameModeProcessor(this);
    private final PingProcessor     pingProcessor     = new PingProcessor(this);
    private final TimingHandler     timingProcessor   = new TimingProcessor(this);

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

    /**
     * Retrieves the entity ID associated with the player.
     *
     * @return the entity ID as an integer
     */
    @Override
    public int entityId() {
        return user.getEntityId();
    }

    @Override
    public int ping() {
        return (int) this.getPingProcessor().getPing();
    }

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
        return user.getClientVersion().name();
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
        Sierra.getPlugin().getLogger().log(Level.INFO, "We disconnect " + this.username() + " for security purpose");
        Sierra.getPlugin().getLogger().log(Level.INFO, "Exception: " + exception.getMessage());
        this.kick();
    }

    /**
     * Executes a punishment action based on the given punish type.
     * If the punish type is BAN and the ban feature is enabled, the player will be banned.
     * In any case, the player will be kicked.
     *
     * @param punishType the type of punishment to be applied
     */
    public void punish(PunishType punishType) {
        setReceivedPunishment(true);
        if (punishType == PunishType.BAN && Sierra.getPlugin().getPunishmentConfig().isBan()) {
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
            Sierra.getPlugin()
                .getSierraConfigEngine()
                .config()
                .getString("punish-command", "ban {username} Crashing")
                .replace("{username}", this.user.getName())
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
}
