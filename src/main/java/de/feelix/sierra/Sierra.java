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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

@Getter
@Setter
public final class Sierra extends JavaPlugin implements SierraApi {

    @Getter
    private static Sierra plugin;
    public static  String PREFIX;

    private SierraConfigEngine sierraConfigEngine;
    private Ticker             ticker;
    private DataManager        dataManager;
    private PunishmentConfig   punishmentConfig;

    /**
     * The onLoad function is called when the plugin is loaded.
     * This function should be used to load config files, register events, and other things that need to be done
     * before the plugin can run.
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
            .checkForUpdates(true)
            .bStats(true);
        PacketEvents.getAPI().load();
    }

    /**
     * The onEnable function is called when the plugin is enabled.
     * It loads metrics, registers listeners, sets the prefix for messages sent by Sierra,
     * initializes a Ticker and DataManager object (which are used to manage data),
     * registers commands with Bukkit's command manager (so that they can be executed in-game),
     * initializes PacketEvents' API so that it can be used by other plugins to listen for packets being
     * sent/received, and finally enables Sierra's API.
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
     * The setPrefix function is used to set the prefix for all messages sent by Sierra.
     * It uses the sierraConfig variable, which is an instance of a class that extends
     * Configurable, and calls its getString function with two parameters: &quot;prefix&quot; and
     * &quot;&amp;8▎ &amp;3Sierra &amp;8▏&quot;. The first parameter specifies what key in the config file we
     * want to use.
     * The second parameter specifies what value should be returned if there's no such key in the config file.
     */
    public void setPrefix() {
        PREFIX = ChatColor.translateAlternateColorCodes('&', sierraConfigEngine.config()
            .getString("layout.prefix", "&8▎ &cSierra &8▏"));
    }

    /**
     * The onDisable function is called when the plugin is disabled.
     * It cancels the task that was created in onEnable, and terminates
     * PacketEvents' API. This function should be used to clean up any resources
     * that were allocated during onEnable, such as closing connections to a database or file system.
     */
    @Override
    public void onDisable() {
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
            this.ticker.getTask().cancel();
        }
    }

    /**
     * The userRepository function returns a UserRepository object.
     *
     * @return A userrepository object
     */
    @Override
    public UserRepository userRepository() {
        return this.dataManager;
    }
}