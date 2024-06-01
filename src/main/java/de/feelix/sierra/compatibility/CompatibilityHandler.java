package de.feelix.sierra.compatibility;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.compatibility.impl.DeluxeMenusDescriptor;
import de.feelix.sierra.compatibility.impl.FastAsyncWorldEditDescriptor;
import de.feelix.sierra.compatibility.impl.ProtocolLibDescriptor;

import java.util.ArrayList;
import java.util.List;

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
    }

    /**
     * This method processes the descriptors of various plugins to check for compatibility issues.
     * It iterates over the list of descriptors and calls the checkDescriptorAndLogResults method to check and log the results.
     */
    public void processDescriptors() {
        Sierra.getPlugin().getLogger().info("Looking for compatibility issues...");
        descriptors.forEach(this::checkDescriptorAndLogResults);
    }

    /**
     * Checks the compatibility of a plugin descriptor and logs the results.
     *
     * @param descriptor the plugin descriptor to check
     */
    private void checkDescriptorAndLogResults(Descriptor descriptor) {
        if (descriptor.compatibilityProblematic()) {
            logIssues(descriptor);
            if (descriptor.fixProblems()) {
                Sierra.getPlugin().getLogger().info("We were able to fix the error");
            } else {
                Sierra.getPlugin().getLogger().severe("The error could not be fixed");
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
