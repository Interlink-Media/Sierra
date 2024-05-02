package de.feelix.sierra;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.command.SierraCommand;
import de.feelix.sierra.listener.PacketListener;
import de.feelix.sierra.listener.bukkit.BlockRedstoneListener;
import de.feelix.sierra.manager.config.PunishmentConfig;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.discord.SierraDiscordGateway;
import de.feelix.sierra.manager.modules.SierraModuleGateway;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.Ticker;
import de.feelix.sierra.utilities.update.UpdateChecker;
import de.feelix.sierraapi.SierraApi;
import de.feelix.sierraapi.SierraApiAccessor;
import de.feelix.sierraapi.module.ModuleGateway;
import de.feelix.sierraapi.user.UserRepository;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Sierra class represents the main class for the Sierra plugin.
 * It extends the JavaPlugin class and implements the SierraApi interface.
 * The Sierra plugin provides various functionalities for managing player data and performing punishments.
 *
 * <p>
 * This class initializes various components of the Sierra plugin, registers event listeners, and sets up the command
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
     * The {@code ticker} variable represents an instance of the {@link Ticker} class.
     *
     * <p>
     * The {@code Ticker} class is a timer that runs asynchronously and performs tasks at regular intervals.
     * It maintains a {@code currentTick} count that increments by 1 every tick.
     * It also has a {@code task} that runs every second and performs certain actions.
     * </p>
     *
     * <p>
     * The {@code ticker} variable is part of the {@link Sierra} plugin and can be obtained through the
     * {@link Sierra#getPlugin()} method.
     * </p>
     *
     * @see Ticker
     * @see Sierra#getPlugin()
     */
    private Ticker ticker;

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
     * The ModuleGateway class represents the gateway for accessing different modules and their functionalities.
     */
    private SierraModuleGateway sierraModuleGateway;

    /**
     * The moduleDir variable represents the directory where the module files are located.
     * It is a private field of type File.
     */
    private File moduleDir;

    /**
     * This method is called when the plugin is being enabled.
     * It initializes various components of the Sierra plugin,
     * registers event listeners, and sets up the command executor.
     */
    @Override
    public void onLoad() {
        plugin = this;
        sierraConfigEngine = new SierraConfigEngine();
        updateChecker = new UpdateChecker();

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
            .fullStackTrace(true)
            .kickOnPacketException(sierraConfigEngine.config().getBoolean("kick-on-packet-exception", true))
            .reEncodeByDefault(false)
            .checkForUpdates(false)
            .bStats(true);

        PacketEvents.getAPI().load();
    }

    /**
     * This method is called when the plugin is being enabled.
     * It initializes various components of the Sierra plugin,
     * registers event listeners and sets up the command executor.
     */
    @Override
    public void onEnable() {
        plugin = this;
        long startTime = System.currentTimeMillis();

        int pluginId = 21527; // https://bstats.org/plugin/bukkit/Sierra/21527

        // For compatibility with folia
        new io.github.retrooper.packetevents.bstats.Metrics(this, pluginId);

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener());
        setPrefix();

        this.ticker = new Ticker();
        this.sierraDataManager = new SierraDataManager();

        Objects.requireNonNull(this.getCommand("sierra")).setExecutor(new SierraCommand());

        PacketEvents.getAPI().init();

        // Setup punishment options for sierra
        this.punishmentConfig = PunishmentConfig.valueOf(
            this.sierraConfigEngine.config().getString("internal-punishment-config", "HARD"));

        if (this.sierraConfigEngine.config().getBoolean("block-redstone-loops", true)) {
            Bukkit.getPluginManager().registerEvents(new BlockRedstoneListener(), this);
        }

        this.sierraDiscordGateway.setup();

        checkForUpdate();
        updateChecker.startScheduler();

        this.loadModules();

        long delay = System.currentTimeMillis() - startTime;
        this.getLogger().log(Level.INFO, "Sierra is ready. (Took: " + delay + "ms)");

        // Enable the api
        SierraApiAccessor.setSierraApiInstance(this);
        this.getLogger().log(Level.INFO, "API is ready");
    }

    /**
     * Loads the modules for the Sierra plugin.
     * The method creates a modules directory specific to the plugin, and initializes a {@link SierraModuleGateway}
     * object
     * to manage the loading of modules.
     * <p>
     * The modules directory is created at "./plugins/{plugin_name}/modules". If the directory already exists,
     * no action is taken. If the directory cannot be created, a severe logging message is displayed.
     * <p>
     * After creating the modules directory, the {@link SierraModuleGateway} object is initialized with the directory
     * and the {@link SierraModuleGateway#loadModules()} method is called to load the modules.
     * <p>
     * This method is called when the plugin is being enabled.
     */
    private void loadModules() {
        this.moduleDir = new File("./plugins/" + this.getDescription().getName() + "/modules");
        if (!this.moduleDir.mkdirs() && !this.moduleDir.exists()) {
            this.getLogger().severe("Failed to create modules directory!");
        }
        this.sierraModuleGateway = new SierraModuleGateway(this.moduleDir);
        this.sierraModuleGateway.loadModules();
    }

    /**
     * Checks for updates to the Sierra plugin asynchronously.
     */
    private void checkForUpdate() {
        FoliaCompatUtil.runTaskAsync(Sierra.getPlugin(), () -> {
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
        logger.log(Level.WARNING, "You are using an outdated version of Sierra!");
        logger.log(Level.WARNING, "Please update Sierra to the latest version!");
        String format = "Your version: %s, latest is: %s";
        logger.log(Level.WARNING, String.format(format, localVersion, latestReleaseVersion));
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
        this.sierraModuleGateway.disableModules();
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
     * The {@code moduleGateway()} method returns an instance of ModuleGateway.
     *
     * @return An instance of ModuleGateway.
     * @see ModuleGateway
     * @see Sierra#sierraModuleGateway
     */
    @Override
    public ModuleGateway moduleGateway() {
        return this.sierraModuleGateway;
    }
}