package de.feelix.sierra.manager.init.impl.start;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.init.Initable;
import de.feelix.sierra.manager.storage.SierraDataManager;
import io.github.retrooper.packetevents.bstats.Metrics;

/**
 * The InitBStats class is responsible for initializing the bStats metrics for the Sierra plugin.
 * It implements the Initable interface and overrides the start() method.
 * The start() method sets up metrics for bans, kicks, and active check types.
 *
 * @see Initable
 */
public class InitBStats implements Initable {

    /**
     * The PLUGIN_ID variable represents the ID of the plugin.
     * It is a private, static, and final field of type int.
     */
    private static final int PLUGIN_ID = 21527;

    /**
     * The start() method initializes the bStats metrics for the Sierra plugin.
     * It sets up metrics for bans, kicks, and active check types.
     */
    @Override
    public void start() {
        Metrics metrics = new Metrics(Sierra.getPlugin(), PLUGIN_ID);

        metrics.addCustomChart(new Metrics.SingleLineChart(
            "bans",
            () -> {
                int bans = SierraDataManager.BANS;
                SierraDataManager.BANS = 0;
                return bans;
            }
        ));
        metrics.addCustomChart(new Metrics.SingleLineChart(
            "kicks",
            () -> {
                int kicks = SierraDataManager.KICKS;
                SierraDataManager.KICKS = 0;
                return kicks;
            }
        ));
        metrics.addCustomChart(new Metrics.AdvancedPie(
            "active_check_types",
            () -> SierraDataManager.violationCount
        ));
    }
}
