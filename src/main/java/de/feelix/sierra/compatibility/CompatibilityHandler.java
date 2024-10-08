package de.feelix.sierra.compatibility;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.compatibility.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * The CompatibilityHandler class handles compatibility issues with various plugins.
 * It checks the compatibility of plugin descriptors and logs any issues found.
 * It also attempts to fix the compatibility problems if possible.
 */
public class CompatibilityHandler {

    private final List<Descriptor> descriptors = new ArrayList<>();

    /**
     * The CompatibilityHandler class handles compatibility issues with various plugins.
     * It checks the compatibility of plugin descriptors and logs any issues found.
     * It also attempts to fix the compatibility problems if possible.
     */
    public CompatibilityHandler() {
        descriptors.add(new ProtocolLibDescriptor());
        descriptors.add(new FastAsyncWorldEditDescriptor());
        descriptors.add(new DeluxeMenusDescriptor());
        descriptors.add(new BetterRTPDescriptor());
        descriptors.add(new InfiniteParkourDescriptor());
        descriptors.add(new InsaneAnnouncerDescriptor());
    }

    /**
     * This method processes the descriptors of various plugins to check for compatibility issues.
     * It iterates over the list of descriptors and calls the checkDescriptorAndLogResults method to check and log
     * the results.
     */
    public void processDescriptors() {
        Sierra.getPlugin().getLogger().info("Looking for compatibility issues...");
        descriptors.forEach(this::checkDescriptorAndLogResults);
    }

    /**
     * Checks the compatibility of a plugin descriptor and logs any issues found.
     * If compatibility problems are found, it attempts to fix them and logs the result.
     *
     * @param descriptor the descriptor representing the plugin to be checked
     */
    private void checkDescriptorAndLogResults(Descriptor descriptor) {
        if (descriptor.compatibilityProblematic()) {
            logIssues(descriptor);
            Logger logger = Sierra.getPlugin().getLogger();
            if (descriptor.fixProblems()) {
                logger.info("We were able to fix the error");
            } else {
                logger.severe("The error could not be fixed");
            }
        }
    }

    /**
     * Logs issues related to a plugin descriptor.
     *
     * @param descriptor the descriptor representing the plugin with issues
     */
    private void logIssues(Descriptor descriptor) {
        Sierra.getPlugin().getLogger().info("Found a problem with: " + descriptor.pluginName());
        descriptor.knownProblems().forEach(problem ->
                                               Sierra.getPlugin().getLogger().info(" - " + problem)
        );
    }
}
