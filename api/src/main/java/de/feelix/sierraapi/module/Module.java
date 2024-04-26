package de.feelix.sierraapi.module;

import java.io.File;

/**
 * The Module interface represents a module in the application.
 * It provides methods to retrieve information about the module, such as its name, data folder, enabled state, and module description.
 */
public interface Module {

    /**
     * Returns the name of the module.
     *
     * @return the name of the module
     */
    String moduleName();

    /**
     * Returns the data folder of the module.
     *
     * @return the data folder of the module
     */
    File dataFolder();

    /**
     * Returns the enabled state of the module.
     *
     * @return true if the module is enabled, false otherwise
     */
    boolean enabled();

    /**
     * Retrieves the module description.
     *
     * @return the module description
     */
    ModuleDescription moduleDescription();

    /**
     * Returns the full path of the module, including the name of the module and the path to its data folder.
     *
     * @return the full path of the module
     */
    String fullModulePath();
}
