package de.feelix.sierra.manager.init.impl.start;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.init.Initable;
import de.feelix.sierra.utilities.update.UpdateChecker;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;

import java.util.logging.Logger;

/**
 * The InitUpdateChecker class is responsible for checking for updates to the Sierra plugin.
 * It implements the Initable interface, which defines a start() method for initialization.
 */
public class InitUpdateChecker implements Initable {

    /**
     * Starts the process of checking for updates in the Sierra plugin.
     * It refreshes the new version, checks for updates, and starts the update scheduler.
     */
    @Override
    public void start() {
        FoliaScheduler.getAsyncScheduler().runNow(Sierra.getPlugin(), o -> {
            Sierra.getPlugin().getUpdateChecker().refreshNewVersion();
            checkForUpdate();
            Sierra.getPlugin().getUpdateChecker().startScheduler();
        });
    }

    /**
     * Checks for updates to the Sierra plugin asynchronously.
     */
    private void checkForUpdate() {
        FoliaScheduler.getAsyncScheduler().runNow(Sierra.getPlugin(), o -> {
            String localVersion         = Sierra.getPlugin().getDescription().getVersion();
            String latestReleaseVersion = Sierra.getPlugin().getUpdateChecker().getLatestReleaseVersion();
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
     * Checks if the version of the plugin is protocol.
     *
     * @return true if the version is protocol, false otherwise
     */
    private boolean isVersionInvalid() {
        return Sierra.getPlugin()
            .getUpdateChecker()
            .getLatestReleaseVersion()
            .equalsIgnoreCase(UpdateChecker.UNKNOWN_VERSION);
    }
}
