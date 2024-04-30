package de.feelix.sierraapi.module;

import lombok.Getter;

import java.io.File;
import java.util.logging.Logger;

/**
 * SierraModule is an abstract class that represents a module in the Sierra system.
 * It provides basic functionality for enabling and disabling modules, as well as storing relevant module information.
 */
@Getter
public abstract class SierraModule {

    /**
     * Represents the folder where the module's data will be stored.
     * <p>
     * The data folder is a file that can be used to store any necessary data for the module.
     * It is typically used to store configuration files, temporary files, cached data, and other module-specific data.
     * The data folder is specified when enabling the module using the {@link SierraModule#enable} method.
     * <p>
     * Example usage:
     * SierraModule module = new SierraModule();
     * File dataFolder = new File("path/to/data/folder");
     * module.enable(description, dataFolder, pluginName);
     *
     * @see SierraModule#enable
     */
    private File dataFolder;

    /**
     * The 'enabled' variable represents the state of a module being enabled or disabled.
     * <p>
     * By default, the 'enabled' variable is set to false, which means the module is disabled.
     * When the 'enabled' variable is set to true, it means the module is enabled.
     * <p>
     * Example usage:
     * <pre>{@code
     * SierraModule module = new SierraModule();
     * boolean isEnabled = module.isEnabled();
     *
     * // Output: false
     * System.out.println(isEnabled);
     * }</pre>
     *
     * @see SierraModule#enable(SierraModuleDescription, File, String)
     * @see SierraModule#disable()
     */
    private boolean enabled = false;

    /**
     * Represents the description of a module, containing information about the module.
     * <p>
     * This class includes fields for the name, main class, author, type, and version of the module.
     * It also provides a method to read the content of a web page specified by a URL string.
     */
    private SierraModuleDescription sierraModuleDescription;

    /**
     * The logger variable is an instance of the Logger class.
     * It is used to log messages and events for debugging and error reporting.
     */
    private Logger logger;

    /**
     * The pluginName field stores the name of the plugin that owns the module.
     * It is used to identify the plugin associated with the module.
     * <p>
     * Example usage:
     * <pre>{@code
     * SierraModule module = new MyModule();
     * String name = module.getPluginName();
     *
     * // Output: "MyPlugin"
     * System.out.println(name);
     * }</pre>
     *
     * @see SierraModule
     */
    private String moduleName;

    /**
     * The full path of the module, including the plugin name and the module name.
     * <p>
     * This field stores the full path of the module, which is constructed using the plugin name and the module name.
     * It is used to identify the location of the module within the file system.
     * <p>
     * Example usage:
     * <pre>{@code
     * String modulePath = fullModulePath;
     * // Output: "plugins//MyPlugin//modules//MyModule"
     * System.out.println(modulePath);
     * }</pre>
     *
     * @see SierraModule#enable(SierraModuleDescription, File, String)
     * @see SierraModuleDescription#getName()
     */
    private String fullModulePath;

    /**
     * Disables the module.
     * <p>
     * This method is called to disable the module. It sets the 'enabled' field to false and clears the 'dataFolder'
     * field.
     * Additionally, it calls the onDisable() method, which can be overridden in a subclass to perform custom logic
     * when the module is disabled.
     * <p>
     * Example usage:
     * <pre>{@code
     * MyModule module = new MyModule();
     * module.disable();
     * }</pre>
     */
    public final void disable() {
        this.onDisable();

        enabled = false;
        dataFolder = null;
    }

    /**
     * Enables the module with the given description, data folder, and plugin name.
     *
     * @param description the ModuleDescription object representing the module
     * @param dataFolder  the folder where the module's data will be stored
     * @param pluginName  the name of the plugin that owns the module
     */
    public final void enable(SierraModuleDescription description, File dataFolder, String pluginName, Logger logger) {
        enabled = true;
        this.sierraModuleDescription = description;
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.moduleName = pluginName;
        this.fullModulePath = "plugins//" + pluginName + "//modules//" + description.getName();
        this.onEnable();
    }

    public abstract void onEnable();

    public abstract void onDisable();
}