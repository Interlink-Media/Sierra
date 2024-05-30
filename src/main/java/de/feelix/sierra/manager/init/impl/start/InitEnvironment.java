package de.feelix.sierra.manager.init.impl.start;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.listener.bukkit.BlockRedstoneListener;
import de.feelix.sierra.manager.config.PunishmentConfig;
import de.feelix.sierra.manager.init.Initable;
import de.feelix.sierra.utilities.message.ConfigValue;
import org.bukkit.Bukkit;

/**
 * The InitEnvironment class implements the Initable interface and represents the initialization of the environment for the Sierra plugin.
 * It sets the punishment configuration and registers the BlockRedstoneListener if the "block-redstone-loops" configuration option is set to true.
 *
 * <p>
 * Example Usage:
 * InitEnvironment initEnvironment = new InitEnvironment();
 * initEnvironment.start();
 * </p>
 *
 * @see Initable
 */
public class InitEnvironment implements Initable {

    /**
     * The start method initializes various components of the Sierra plugin and registers event listeners.
     * It sets the punishment configuration and registers the block redstone listener if the corresponding configuration option is enabled.
     */
    @Override
    public void start() {
        Sierra.getPlugin().setPunishmentConfig(PunishmentConfig.valueOf(
            new ConfigValue("internal-punishment-config", "HARD", false).message()));

        if (Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("block-redstone-loops", true)) {
            Bukkit.getPluginManager().registerEvents(new BlockRedstoneListener(), Sierra.getPlugin());
        }
    }
}
