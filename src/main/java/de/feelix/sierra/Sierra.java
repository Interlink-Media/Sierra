package de.feelix.sierra;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.manager.event.AbstractEventBus;
import de.feelix.sierra.command.SierraCommand;
import de.feelix.sierra.listener.PacketListener;
import de.feelix.sierra.listener.bukkit.BlockRedstoneListener;
import de.feelix.sierra.manager.config.PunishmentConfig;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.discord.SierraDiscordGateway;
import de.feelix.sierra.manager.server.SierraServerManager;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.Ticker;
import de.feelix.sierra.utilities.update.UpdateChecker;
import de.feelix.sierraapi.SierraApi;
import de.feelix.sierraapi.SierraApiAccessor;
import de.feelix.sierraapi.events.EventBus;
import de.feelix.sierraapi.server.SierraServer;
import de.feelix.sierraapi.user.UserRepository;
import io.github.retrooper.packetevents.bstats.Metrics;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Sierra class represents the main class for the Sierra plugin.
 * It extends the JavaPlugin class and implements the SierraApi interface.
 * The Sierra plugin provides various functionalities for managing player data and performing punishments.
 *
 * <p>
 * This class initializes various components of the Sierra plugin, registers event priority, and sets up the command
 * executor.
 * It also provides methods for accessing and manipulating player data and punishment configuration.
 * </p>
 *
 * @see JavaPlugin
 * @see SierraApi
 */
@Getter
@Setter
public final class Sierra extends JavaPlugin implements SierraApi {

    /**
     * This variable represents the instance of the Sierra plugin.
     * It is a static, private field with a getter method.
     */
    @Getter
    private static Sierra plugin;

    /**
     * The PREFIX variable represents the prefix for messages sent by the Sierra plugin.
     * It is obtained from the "layout.prefix" configuration option in the sierra.yml file.
     * The prefix is translated using the '&' character as a color code indicator.
     */
    public static String PREFIX;

    /**
     * SierraConfigEngine is a class that manages the main configuration file for the Sierra plugin.
     * It provides various methods to access and manipulate the configuration options.
     */
    private SierraConfigEngine sierraConfigEngine;

    /**
     * The DataManager class represents a singleton instance that manages player data in the application.
     * It provides methods to manipulate and retrieve player data from the underlying data structures.
     * <p>
     * This variable is an instance of the DataManager class, used to manage player data in the application.
     *
     * @see SierraDataManager
     */
    private SierraDataManager sierraDataManager;

    /**
     * The PunishmentConfig enum represents the configuration options for punishments in a system.
     * It defines whether a user should be kicked or banned based on certain criteria.
     */
    private PunishmentConfig punishmentConfig;

    /**
     * Represents an update checker for checking the latest release version of a given repository on GitHub.
     */
    private UpdateChecker updateChecker;

    /**
     * Represents a Discord gateway for sending alerts and messages through a webhook.
     * The DiscordGateway class uses a webhook URL to interact with Discord.
     * It can be used to set up the gateway, send alerts, and perform other actions related to Discord integration.
     */
    private SierraDiscordGateway sierraDiscordGateway = new SierraDiscordGateway();

    /**
     * Represents an event bus that allows events to be published and subscribed to.
     */
    private final EventBus eventBus = new AbstractEventBus();

    /**
     * The Server interface represents a server and defines its properties and behaviors.
     */
    private final SierraServer sierraServer = new SierraServerManager();

    /**
     * The PLUGIN_ID variable represents the unique identifier for the plugin. It is an integer value.
     * <p>
     * Note: This documentation does not include example code or author/version tags.
     */
    private static final int PLUGIN_ID = 21527;

    /**
     * This method is called when the plugin is being enabled.
     * It initializes various components of the Sierra plugin,
     * registers event priority, and sets up the command executor.
     */
    @Override
    public void onLoad() {
        plugin = this;
        sierraConfigEngine = new SierraConfigEngine();
        updateChecker = new UpdateChecker();
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        configureApiSettings();
        PacketEvents.getAPI().load();
    }

    /**
     * The configureApiSettings method configures the settings of the PacketEvents API used by the Sierra plugin.
     * This method modifies various settings such as full stack trace, kick on packet exception, re-encoding,
     * check for updates, and bStats.
     * <p>
     * This method retrieves the PacketEvents API from the SierraApi using the getAPI() method.
     * It then uses the fluent builder pattern to configure the settings of the API.
     * <p>
     * Example usage:
     * <pre>{@code
     * configureApiSettings();
     * }</pre>
     */
    private void configureApiSettings() {
        PacketEvents.getAPI().getSettings()
            .fullStackTrace(true)
            .kickOnPacketException(sierraConfigEngine.config().getBoolean("kick-on-packet-exception", true))
            .reEncodeByDefault(false)
            .checkForUpdates(false)
            .bStats(true);
    }

    /**
     * This method is called when the plugin is being enabled.
     * It initializes various components of the Sierra plugin,
     * registers event priority, and sets up the command executor.
     */
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        initializePacketEvents();
        setPrefix();
        new Ticker();

        this.sierraDataManager = new SierraDataManager();
        Objects.requireNonNull(this.getCommand("sierra")).setExecutor(new SierraCommand());

        setupPunishmentConfig();
        blockRedStoneLoops();

        this.sierraDiscordGateway.setup();

        FoliaScheduler.getAsyncScheduler().runNow(this, o -> checkAndUpdatePlugin());

        long delay = System.currentTimeMillis() - startTime;
        logInitializationTime(delay);

        SierraApiAccessor.setSierraApiInstance(this);
        this.getLogger().log(Level.INFO, "API is ready");
    }

    /**
     * Initializes the packet events for the Sierra plugin.
     * This method sets up the necessary components for packet event handling, such as metrics, event listener
     * registration,
     * and initialization of the PacketEvents API.
     */
    private void initializePacketEvents() {
        Metrics metrics = new Metrics(this, PLUGIN_ID);

        metrics.addCustomChart(new Metrics.SingleLineChart("bans", () -> SierraDataManager.BANS));
        metrics.addCustomChart(new Metrics.SingleLineChart("kicks", () -> SierraDataManager.KICKS));

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener());
        PacketEvents.getAPI().init();
    }

    /**
     * The setupPunishmentConfig method is a private helper method used to set up the punishment configuration for
     * the Sierra plugin.
     * It retrieves the punishment configuration value from the sierra.yml configuration file, and sets the
     * punishmentConfig field
     * in the Sierra class using the PunishmentConfig enum.
     * <p>
     * The punishment configuration is obtained from the "internal.punishment.config" configuration option in the
     * sierra.yml file.
     * If this option does not exist in the configuration file, the "hard" punishment configuration is used as the
     * default value.
     * <p>
     * Example usage:
     * <pre>{@code
     * setupPunishmentConfig();
     * }</pre>
     *
     * @see Sierra#sierraConfigEngine
     * @see PunishmentConfig
     */
    private void setupPunishmentConfig() {
        this.punishmentConfig = PunishmentConfig.valueOf(
            this.sierraConfigEngine.config().getString("internal-punishment-config", "HARD"));
    }

    /**
     * This method blocks redstone loops if the "BLOCK_REDSTONE_LOOP" configuration option is set to true.
     * It registers the BlockRedstoneListener event listener with the Bukkit plugin manager.
     *
     * @see Sierra#sierraConfigEngine
     * @see SierraConfigEngine#config()
     * @see BlockRedstoneListener
     */
    private void blockRedStoneLoops() {
        if (this.sierraConfigEngine.config().getBoolean("block-redstone-loops", true)) {
            Bukkit.getPluginManager().registerEvents(new BlockRedstoneListener(), this);
        }
    }

    /**
     * Check for updates to the plugin and start the update checker scheduler.
     */
    private void checkAndUpdatePlugin() {
        updateChecker.refreshNewVersion();
        checkForUpdate();
        updateChecker.startScheduler();
    }

    /**
     * Logs the initialization time of the Sierra plugin.
     *
     * @param delay the time taken for initialization in milliseconds
     */
    private void logInitializationTime(long delay) {
        this.getLogger().info("Sierra is ready. (Took: " + delay + "ms)");
    }

    /**
     * Checks for updates to the Sierra plugin asynchronously.
     */
    private void checkForUpdate() {
        FoliaScheduler.getAsyncScheduler().runNow(Sierra.getPlugin(), o -> {
            String localVersion         = Sierra.getPlugin().getDescription().getVersion();
            String latestReleaseVersion = updateChecker.getLatestReleaseVersion();
            if (!localVersion.equalsIgnoreCase(latestReleaseVersion) && !isVersionInvalid()) {
                logOutdatedVersionMessage(localVersion, latestReleaseVersion);
            }
        });
    }

    /**
     * Logs a warning message indicating that the local version of Sierra is outdated and suggests updating to the
     * latest version.
     *
     * @param localVersion         the current version of Sierra being used
     * @param latestReleaseVersion the latest release version of Sierra available
     */
    private void logOutdatedVersionMessage(String localVersion, String latestReleaseVersion) {
        Logger logger = Sierra.getPlugin().getLogger();
        logger.warning("You are using an outdated version of Sierra!");
        logger.warning("Please update Sierra to the latest version!");
        String format = "Your version: %s, latest is: %s";
        logger.warning(String.format(format, localVersion, latestReleaseVersion));
    }

    /**
     * Checks if the version of the plugin is invalid.
     *
     * @return true if the version is invalid, false otherwise
     */
    private boolean isVersionInvalid() {
        return this.updateChecker.getLatestReleaseVersion().equalsIgnoreCase(UpdateChecker.UNKNOWN_VERSION);
    }

    /**
     * Sets the prefix for messages sent by the Sierra plugin.
     * The prefix is obtained from the "layout.prefix" configuration option in the sierra.yml file.
     * The prefix is translated using the '&' character as a color code indicator.
     */
    public void setPrefix() {
        PREFIX = ChatColor.translateAlternateColorCodes('&', sierraConfigEngine.config()
            .getString("layout.prefix", "&8▎ &cSierra &8▏"));
    }

    /**
     * This method is called when the plugin is being disabled.
     * It terminates the PacketEvents API if it is not null
     * and cancels the ticker task.
     */
    @Override
    public void onDisable() {
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
    }

    /**
     * Returns the UserRepository implementation used by the Sierra plugin.
     *
     * @return the UserRepository implementation
     * @see UserRepository
     */
    @Override
    public UserRepository userRepository() {
        return this.sierraDataManager;
    }

    /**
     * Returns the EventBus implementation used by the Sierra plugin.
     *
     * @return the EventBus implementation
     * @see EventBus
     */
    @Override
    public EventBus eventBus() {
        return this.eventBus;
    }

    /**
     * The server method returns the Server instance representing the server.
     *
     * @return the Server instance representing the server.
     * @see SierraServer
     */
    @Override
    public SierraServer server() {
        return sierraServer;
    }
}