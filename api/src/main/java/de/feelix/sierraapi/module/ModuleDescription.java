package de.feelix.sierraapi.module;

/**
 * The ModuleDescription interface represents the description of a module.
 * It provides information about the module's name, main class, author, and version.
 */
public interface ModuleDescription {

    /**
     * Returns the name of the module.
     *
     * @return the name of the module
     */
    String name();

    /**
     * Returns the value of the main method of the module.
     *
     * @return the value of the main method
     */
    String main();

    /**
     * Returns the author of the module.
     *
     * @return the author of the module
     */
    String author();

    /**
     * Returns the version of the module.
     *
     * @return the version of the module
     */
    String version();
}
