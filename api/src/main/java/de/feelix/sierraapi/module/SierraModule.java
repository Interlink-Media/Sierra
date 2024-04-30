package de.feelix.sierraapi.module;

import de.feelix.sierraapi.SierraApi;
import de.feelix.sierraapi.SierraApiAccessor;
import de.feelix.sierraapi.exceptions.impl.SierraNotLoadedException;
import lombok.Getter;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.logging.Logger;

/**
 * SierraModule is an abstract class that represents a module in the Sierra system.
 * It provides basic functionality for enabling and disabling modules, as well as storing relevant module information.
 */
@SuppressWarnings("unused")
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
     * Represents the enabled state of a module.
     * <p>
     * The enabled state determines whether a module is active or not.
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
     * The fullModulePath is a private instance variable of type String that stores the full path of a module.
     * It represents the location of the module's files or resources on the filesystem.
     * The fullModulePath is used internally within the SierraModule class and is not meant to be accessed directly from outside the class.
     * The value of the fullModulePath variable should be set when the module is enabled and may be cleared when the module is disabled.
     * The fullModulePath variable is initialized by the enable() method of the SierraModule class, which takes in a SierraModuleDescription object, a dataFolder File object, a plugin
     * Name String, and a logger Logger object as parameters.
     * The value of the fullModulePath variable may be modified or used by other methods within the SierraModule class, depending on the specific functionality of the module.
     * The fullModulePath variable is declared as private, indicating that it can only be accessed within the same class (SierraModule).
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
    public void disable() {
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
    public void enable(SierraModuleDescription description, File dataFolder, String pluginName, Logger logger) {
        enabled = true;
        this.sierraModuleDescription = description;
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.moduleName = pluginName;
        this.fullModulePath = "plugins//" + pluginName + "//modules//" + description.getName();
        this.onEnable();
    }

    /**
     * This method returns a WeakReference to the instance of SierraApi.
     *
     * @return a WeakReference to the SierraApi instance
     * @throws SierraNotLoadedException if SierraApi is not loaded
     */
    public WeakReference<SierraApi> sierraApi() {
        return SierraApiAccessor.access();
    }

    /**
     * This method is called when the module is enabled. It should be overridden in a subclass to perform custom logic
     * when the module is enabled.
     * <p>
     * Note: This method is called automatically from the enable() method in the SierraModule class.
     * <p>
     * Example usage:
     * <pre>{@code
     * public class MyModule extends SierraModule {
     *     public void onEnable() {
     *         // Perform custom logic when the module is enabled
     *     }
     * }
     * }</pre>
     */
    public abstract void onEnable();

    /**
     * Disables the module.
     * <p>
     * This method is called to disable the module. It should be overridden in a subclass to perform custom logic
     * when the module is disabled.
     * <p>
     * Example usage:
     * <p>
     *     MyModule module = new MyModule();
     *     module.disable();
     */
    public abstract void onDisable();
}
