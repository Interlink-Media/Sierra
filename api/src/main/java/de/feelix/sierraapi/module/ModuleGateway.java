package de.feelix.sierraapi.module;

import java.io.File;
import java.util.Map;

/**
 * ModuleGateway is an interface for managing modules in the Sierra plugin.
 */
public interface ModuleGateway {

    /**
     * Returns the directory where the module is located.
     *
     * @return The directory where the module is located.
     */
    File moduleDirectory();

    /**
     * Retrieves a map of module names to module objects.
     *
     * @return A map of module names to module objects.
     */
    Map<String, Module> modules();

    /**
     * Checks if a module with the given name is activated.
     *
     * @param moduleName The name of the module to check.
     * @return true if the module is activated, false otherwise.
     */
    boolean moduleActivated(String moduleName);

    /**
     * Deactivates a module with the given moduleName.
     *
     * @param moduleName The name of the module to deactivate.
     * @return true if the module was successfully deactivated, false otherwise.
     */
    boolean deactivateModule(String moduleName);

    /**
     * Activates a module with the given moduleName.
     *
     * @param moduleName The name of the module to activate.
     * @return true if the module was successfully activated, false otherwise.
     */
    boolean activateModule(String moduleName);
}
