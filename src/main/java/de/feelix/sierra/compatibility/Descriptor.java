package de.feelix.sierra.compatibility;

import java.util.List;

/**
 * The Descriptor interface represents a plugin descriptor.
 * It provides information about the plugin, known problems, and compatibility issues.
 */
public interface Descriptor {

    /**
     * Returns the name of the plugin.
     *
     * @return the name of the plugin
     */
    String pluginName();

    /**
     * Returns a list of known problems for the plugin.
     *
     * @return a list of known problems as a List of Strings
     * @see <a href="https://github.com/example/repo">More information</a>
     */
    List<String> knownProblems();

    /**
     * Attempts to fix the problems identified by the compatibility check.
     *
     * @return true if the problems were successfully fixed, false otherwise
     */
    boolean fixProblems();

    /**
     * Checks if there are any compatibility problems with the plugin.
     *
     * @return true if there are compatibility problems, false otherwise
     */
    boolean compatibilityProblematic();
}
