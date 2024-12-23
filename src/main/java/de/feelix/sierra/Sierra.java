package de.feelix.sierra;

import de.feelix.sierra.compatibility.CompatibilityHandler;
import de.feelix.sierra.manager.event.AbstractEventBus;
import de.feelix.sierra.manager.config.PunishmentConfig;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.init.InitManager;
import de.feelix.sierra.manager.server.SierraServerManager;
import de.feelix.sierra.manager.storage.AddressStorage;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.message.ConfigValue;
import de.feelix.sierra.utilities.update.UpdateChecker;
import de.feelix.sierraapi.LoaderAPI;
import de.feelix.sierraapi.SierraApi;
import de.feelix.sierraapi.SierraApiAccessor;
import de.feelix.sierraapi.events.EventBus;
import de.feelix.sierraapi.server.SierraServer;
import de.feelix.sierraapi.user.UserRepository;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;

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
     * compatibilityHandler is an instance of the CompatibilityHandler class. It is responsible for checking compatibility issues with various plugins.
     *
     * @see CompatibilityHandler
     */
    private CompatibilityHandler compatibilityHandler = new CompatibilityHandler();

    /**
     * Represents an event bus that allows events to be published and subscribed to.
     */
    private final EventBus eventBus = new AbstractEventBus();

    /**
     * The Server interface represents a server and defines its properties and behaviors.
     */
    private final SierraServer sierraServer = new SierraServerManager();

    /**
     * The initManager variable is an instance of the InitManager class.
     * It is responsible for managing the initialization of various components in the Sierra plugin.
     * The InitManager class has three sets of initializers: initializersOnLoad, initializersOnStart, and initializersOnStop.
     * Each set contains instances of classes that implement the Initable interface.
     * <p>
     * The initializersOnLoad set contains initializers that are executed when the plugin is being loaded.
     * The initializersOnStart set contains initializers that are executed when the plugin is being enabled.
     * The initializersOnStop set contains initializers that are executed when the plugin is being disabled.
     * <p>
     * Example Usage:
     * InitManager initManager = new InitManager();
     * initManager.load();
     * initManager.start();
     * initManager.stop();
     *
     * @see InitManager
     */
    private final InitManager initManager = new InitManager();

    /**
     * The AddressStorage class is responsible for storing and managing IP addresses
     * along with their corresponding added time.
     */
    private AddressStorage addressStorage = new AddressStorage();

    /**
     * This method is called when the plugin is being enabled.
     * It initializes various components of the Sierra plugin,
     * registers event priority, and sets up the command executor.
     */
    @Override
    public void onLoad() {
        plugin = this;
        sierraConfigEngine = new SierraConfigEngine();
        initManager.load();
        updateChecker = new UpdateChecker();
    }

    /**
     * This method is called when the plugin is being enabled.
     * It initializes various components of the Sierra plugin,
     * registers event priority, and sets up the command executor.
     */
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        this.sierraDataManager = new SierraDataManager();
        setPrefix();
        initManager.start();

        ViaVersionUtil.checkIfViaIsPresent();

        SierraApiAccessor.setSierraApiInstance(this);
        this.getLogger().info("API is ready");
        LoaderAPI.triggerCallbacks();

        long delay = System.currentTimeMillis() - startTime;
        this.getLogger().info("Sierra is ready. (Took: " + delay + "ms)");
        this.compatibilityHandler.processDescriptors();
    }

    /**
     * Sets the prefix for messages sent by the Sierra plugin.
     * The prefix is obtained from the "layout.prefix" configuration option in the sierra.yml file.
     * The prefix is translated using the '&' character as a color code indicator.
     */
    public void setPrefix() {
        PREFIX = new ConfigValue("layout.prefix", "&3Sierra &7>", true)
            .colorize().message();
    }

    /**
     * This method is called when the plugin is being disabled.
     * It terminates the PacketEvents API if it is not null
     * and cancels the ticker task.
     */
    @Override
    public void onDisable() {
        this.initManager.stop();
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
