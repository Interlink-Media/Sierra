package de.feelix.sierra;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.command.SierraCommand;
import de.feelix.sierra.listener.PacketListener;
import de.feelix.sierra.manager.config.PunishmentConfig;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.storage.DataManager;
import de.feelix.sierra.utilities.Ticker;
import de.feelix.sierra.utilities.bstats.Metrics;
import de.feelix.sierraapi.SierraApi;
import de.feelix.sierraapi.SierraApiAccessor;
import de.feelix.sierraapi.user.UserRepository;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

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
    public static  String PREFIX;

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
     * The {@code ticker} variable is part of the {@link Sierra} plugin and can be obtained through the {@link Sierra#getPlugin()} method.
     * </p>
     *
     * @see Ticker
     * @see Sierra#getPlugin()
     */
    private Ticker             ticker;

    /**
     * The DataManager class represents a singleton instance that manages player data in the application.
     * It provides methods to manipulate and retrieve player data from the underlying data structures.
     * <p>
     * This variable is an instance of the DataManager class, used to manage player data in the application.
     *
     * @see DataManager
     */
    private DataManager        dataManager;

    /**
     * The PunishmentConfig enum represents the configuration options for punishments in a system.
     * It defines whether a user should be kicked or banned based on certain criteria.
     */
    private PunishmentConfig   punishmentConfig;

    /**
     * This method is called when the plugin is being enabled.
     * It initializes various components of the Sierra plugin,
     * registers event listeners, and sets up the command executor.
     */
    @Override
    public void onLoad() {
        plugin = this;
        sierraConfigEngine = new SierraConfigEngine();

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
            .fullStackTrace(true)
            .reEncodeByDefault(false)
            .kickOnPacketException(true)
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
        new Metrics(this, pluginId);

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener());
        setPrefix();

        this.ticker = new Ticker();
        this.dataManager = new DataManager();

        Objects.requireNonNull(this.getCommand("sierra")).setExecutor(new SierraCommand());

        PacketEvents.getAPI().init();

        // Setup punishment options for sierra
        this.punishmentConfig = PunishmentConfig.valueOf(
            this.sierraConfigEngine.config().getString("internal-punishment-config", "HARD"));

        this.getLogger().log(
            Level.INFO,
            "Sierra is ready. (Took: " + (System.currentTimeMillis() - startTime) + "ms)"
        );

        // Enable the api
        SierraApiAccessor.setSierraApiInstance(this);
        this.getLogger().log(Level.INFO, "API is ready");
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
            this.ticker.getTask().cancel();
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
        return this.dataManager;
    }
}