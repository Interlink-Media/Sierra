package de.feelix.sierra.manager.modules;

import de.feelix.sierra.Sierra;
import de.feelix.sierraapi.module.ModuleGateway;
import de.feelix.sierraapi.module.SierraModule;
import de.feelix.sierraapi.module.SierraModuleDescription;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The ModuleGateway class is responsible for managing the loading and disabling of modules.
 */
@Getter
public class SierraModuleGateway implements ModuleGateway {

    /**
     * The ModuleGateway class represents a gateway for accessing and managing modules in the Sierra system.
     * It provides methods for loading, enabling, and disabling modules.
     */
    public static final Map<String, SierraModule> modules = new HashMap<>();

    /**
     * The directory where the module files are located.
     */
    private final File moduleDir;

    /**
     * This class represents the ModuleGateway for managing modules in the Sierra plugin.
     *
     * <p>
     * The ModuleGateway class is responsible for initializing and managing modules in the plugin. It provides
     * methods for loading and disabling modules.
     * </p>
     *
     * @param moduleDir the directory where the modules are located
     */
    public SierraModuleGateway(File moduleDir) {
        this.moduleDir = moduleDir;

        if (!moduleDir.exists()) {
            if (!moduleDir.mkdirs()) {
                Sierra.getPlugin().getLogger().severe("Failed to create module directory: " + moduleDir);
            }
        }
    }

    /**
     * Disables all modules in the Sierra plugin.
     *
     * <p>
     * Disabling modules involves calling the disable() method of each module,
     * removing them from the modules map, and clearing the map.
     * </p>
     */
    public void disableModules() {
        Sierra.getPlugin().getLogger().info("Disabling Modules...");
        for (final String name : modules.keySet()) {
            final SierraModule            module      = modules.get(name);
            final SierraModuleDescription description = module.getSierraModuleDescription();
            Sierra.getPlugin()
                .getLogger()
                .info(String.format("Disabling Module %s v%s by %s...", description.getName(), description.getVersion(),
                                    description.getAuthor()
                ));
            module.disable();
        }
        modules.clear();
    }

    /**
     * Loads a file as a module.
     *
     * @param file the file to load as a module
     */
    public void load(File file) {
        if (isJarFile(file)) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);
                JarEntry jarEntry = jarFile.getJarEntry("module.properties");
                if (jarEntry == null) {
                    logSevere(String.format("File '%s' does not contains module.properties file!", file.getName()));
                    return;
                }
                processJarEntry(jarFile, jarEntry, file);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {
                    if (jarFile != null) {
                        jarFile.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Checks if a given file is a JAR file.
     *
     * @param file the file to check
     * @return true if the file is a JAR file, false otherwise
     */
    private boolean isJarFile(File file) {
        return file.getName().endsWith(".jar");
    }

    /**
     * Processes a JarEntry in a JarFile and performs necessary operations.
     *
     * @param jarFile  the JarFile containing the specified JarEntry
     * @param jarEntry the JarEntry to be processed
     * @param file     the corresponding File object of the JarFile
     */
    private void processJarEntry(JarFile jarFile, JarEntry jarEntry, File file) {
        try (InputStreamReader isr = new InputStreamReader(jarFile.getInputStream(jarEntry), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(isr);
            processModuleClassLoader(file, new SierraModuleDescription(properties), getModuleClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the ClassLoader for the module.
     * <p>
     * This method returns the ClassLoader associated with the module in the Sierra plugin. The ClassLoader is obtained
     * from the Sierra plugin instance using the getClass().getClassLoader() method.
     *
     * @return the ClassLoader for the module
     */
    private ClassLoader getModuleClassLoader() {
        return Sierra.getPlugin().getClass().getClassLoader();
    }

    /**
     * Process the module class loader for a given module file and module description.
     *
     * @param file              The file representing the module.
     * @param description       The description of the module.
     * @param moduleClassLoader The class loader for the module.
     * @throws MalformedURLException  If the URL of the file is invalid.
     * @throws ClassNotFoundException If the main class specified in the description cannot be found.
     */
    private void processModuleClassLoader(File file, SierraModuleDescription description, ClassLoader moduleClassLoader)
        throws MalformedURLException, ClassNotFoundException {
        final URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, moduleClassLoader);
        final Class<?>       main   = Class.forName(description.getMain(), false, loader);
        if (main.getSuperclass() == SierraModule.class) {
            processSierraModule(main, description);
        } else {
            String rawString = "Class %s in Module %s does not extend 'Module'!";
            logSevere(String.format(rawString, description.getMain(), file.getName()));
        }
    }

    /**
     * Processes a Sierra module by instantiating the main class, enabling the module, and updating its status in the
     * modules map.
     * <p>
     * This method creates an instance of the main class using reflection, based on the provided Class object. It
     * then logs the module's name, version, and author as part of the enable
     * ment process. The module is added to the modules map, with its name as the key. The method then calls
     * processDataDirectory() to perform necessary operations on the module's
     * data directory, followed by calling the enable() method of the module, passing the ModuleDescription, module
     * directory File, and the name of the Sierra plugin's description
     * as parameters.
     * </p>
     * <p>
     * If any exception occurs during the process, it is printed to the standard error stream.
     * </p>
     *
     * @param main        the Class object representing the main class of the Sierra module
     * @param description the ModuleDescription object representing the description of the Sierra module
     */
    private void processSierraModule(Class<?> main, SierraModuleDescription description) {
        try {
            final SierraModule module    = (SierraModule) main.getDeclaredConstructors()[0].newInstance();
            String             rawString = "Enabling Module %s version=%s, by=%s";

            logInfo(String.format(rawString, description.getName(), description.getVersion(), description.getAuthor()));
            modules.put(description.getName(), module);
            processDataDirectory(description);
            module.enable(
                description, new File(moduleDir, description.getName()), Sierra.getPlugin().getDescription().getName(),
                Sierra.getPlugin().getLogger()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes the data directory for the given module.
     *
     * <p>
     * This method creates a data directory for the module specified in the {@code ModuleDescription} parameter.
     * The data directory is created under the module directory represented by the {@code moduleDir} field.
     * If the data directory already exists or cannot be created, an error message is logged using the {@code
     * logSevere} method.
     * </p>
     *
     * @param description the {@code ModuleDescription} object representing the module to process the data directory for
     */
    private void processDataDirectory(SierraModuleDescription description) {
        final File dataDir = new File(moduleDir, description.getName());
        if (!dataDir.mkdir()) {
            logSevere("Failed to create data directory: " + dataDir);
        }
    }

    /**
     * Logs an informational message.
     *
     * @param message the message to be logged
     */
    private void logInfo(String message) {
        Sierra.getPlugin().getLogger().info(message);
    }

    /**
     * Logs a severe level message.
     *
     * @param message the message to be logged
     */
    private void logSevere(String message) {
        Sierra.getPlugin().getLogger().severe(message);
    }

    /**
     * Loads all modules from the specified directory.
     *
     * <p>
     * This method is responsible for loading modules from the directory specified by the moduleDir field.
     * It iterates over each file in the directory and loads it as a module if it is a valid module file.
     * </p>
     *
     * <p>
     * This method logs an informational message indicating that the modules are being loaded.
     * </p>
     */
    public void loadModules() {
        Sierra.getPlugin().getLogger().info("Loading modules...");
        for (final File file : Objects.requireNonNull(moduleDir.listFiles())) {
            if (isValidModuleFile(file)) {
                load(file);
            }
        }
    }

    /**
     * Checks whether the given file is a valid module file. A file is considered valid if it is not a directory.
     *
     * @param file the file to verify
     * @return true if the file is a valid module file, false otherwise
     */
    private boolean isValidModuleFile(File file) {
        return !file.isDirectory();
    }

    /**
     * Returns the directory where the modules are located.
     *
     * @return the directory where the modules are located
     */
    @Override
    public File moduleDirectory() {
        return this.moduleDir;
    }

    /**
     * Returns the map of modules.
     *
     * @return a map of modules, where the module name is the key and the Module object is the value
     */
    @Override
    public Map<String, SierraModule> modules() {
        return modules;
    }

    /**
     * Retrieves a module by its name.
     *
     * @param name the name of the module to retrieve
     * @return an AtomicReference of SierraModule representing the module with the specified name, or null if not found
     */
    @Override
    public AtomicReference<SierraModule> getModule(String name) {
        AtomicReference<SierraModule> moduleRef = new AtomicReference<>();

        modules.forEach((s, sierraModule) -> {
            if (s.equalsIgnoreCase(name)) {
                moduleRef.set(sierraModule);
            }
        });
        return moduleRef;
    }

    /**
     * Checks if a module is activated.
     *
     * @param moduleName the name of the module to check
     * @return true if the module is activated, false otherwise
     */
    @Override
    public boolean moduleActivated(String moduleName) {
        for (SierraModule value : modules.values()) {
            if (value.getModuleName().equalsIgnoreCase(moduleName)) {
                return value.isEnabled();
            }
        }
        return false;
    }

    /**
     * Deactivates the module with the specified module name.
     *
     * @param moduleName the name of the module to deactivate
     * @return true if the module is successfully deactivated, false if the module is not found
     * @throws RuntimeException if the module is already deactivated
     */
    @Override
    public boolean deactivateModule(String moduleName) {
        for (SierraModule value : modules.values()) {
            if (value.getModuleName().equalsIgnoreCase(moduleName)) {

                if (!value.isEnabled()) throw new RuntimeException("Module " + moduleName + " is already deactivated");

                value.disable();
                return true;
            }
        }
        return false;
    }

    /**
     * Activates a module with the given module name.
     *
     * @param moduleName the name of the module to activate
     * @return true if the module is successfully activated, false if the module is not found or is already activated
     * @throws RuntimeException if the module is already activated
     */
    @Override
    public boolean activateModule(String moduleName) {
        for (SierraModule value : modules.values()) {
            if (value.getModuleName().equalsIgnoreCase(moduleName)) {

                if (value.isEnabled()) throw new RuntimeException("Module " + moduleName + " is already activated");
                value.onEnable();
                return true;
            }
        }
        return false;
    }
}
