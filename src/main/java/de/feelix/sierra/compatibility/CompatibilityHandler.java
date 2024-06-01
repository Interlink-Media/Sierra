package de.feelix.sierra.compatibility;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.compatibility.impl.FastAsyncWorldEditDescriptor;
import de.feelix.sierra.compatibility.impl.ProtocolLibDescriptor;

import java.util.ArrayList;

/**
 * CompatibilityHandler class is responsible for checking compatibility issues with various plugins.
 * It processes a list of descriptors and performs checks to identify and fix compatibility problems.
 */
public class CompatibilityHandler {

    /**
     * The descriptors variable represents a list of Descriptor objects.
     *
     * @see Descriptor
     */
    private final ArrayList<Descriptor> descriptors = new ArrayList<>();

    /**
     * The CompatibilityHandler class is responsible for checking compatibility issues with various plugins.
     * It processes a list of descriptors and performs checks to identify and fix compatibility problems.
     */
    public CompatibilityHandler() {
        this.descriptors.add(new ProtocolLibDescriptor());
        this.descriptors.add(new FastAsyncWorldEditDescriptor());
    }

    /**
     * The processDescriptors method is responsible for checking compatibility issues in a list of descriptors
     * and performing necessary actions to fix the problems.
     */
    public void processDescriptors() {
        Sierra.getPlugin().getLogger().info("Looking for compatibility issues...");
        for (Descriptor descriptor : descriptors) {
            if (descriptor.compatibilityProblematic()) {
                Sierra.getPlugin().getLogger().info("Found a problem with: " + descriptor.pluginName());
                for (String string : descriptor.knownProblems()) {
                    Sierra.getPlugin().getLogger().info(" - " + string);
                }
                if (descriptor.fixProblems()) {
                    Sierra.getPlugin().getLogger().info("We were able to fix the error");
                } else {
                    Sierra.getPlugin().getLogger().severe("The error could not be fixed");
                }
            }
        }
    }
}
