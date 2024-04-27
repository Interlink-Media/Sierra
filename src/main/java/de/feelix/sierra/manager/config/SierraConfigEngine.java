package de.feelix.sierra.manager.config;

import de.feelix.sierra.Sierra;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The SierraConfigEngine class is responsible for managing the configuration files in the Sierra plugin.
 * It provides methods to load the main configuration file and messages file into memory, and retrieve them as YamlConfiguration objects.
 * The loaded files are cached for efficient access.
 */
public class SierraConfigEngine {

    /**
     * The cache variable is a concurrent map that stores the loaded YAML configurations.
     * Each configuration is mapped to a string key representing the name of the configuration file.
     */
    private static final Map<String, YamlConfiguration> cache = new ConcurrentHashMap<>();

    /**
     * The SierraConfigEngine function is a constructor for the SierraConfigEngine class.
     * It loads the main configuration file and messages file into memory, so that they can be accessed by other
     * functions.
     */
    public SierraConfigEngine() {
        //noinspection unused
        YamlConfiguration mainConfig = config();
    }

    /**
     * The config function is a simple way to get the sierra.yml file from the cache,
     * and return it as a YamlConfiguration object. This allows us to easily access all of
     * our configuration options in an easy-to-use format.
     *
     * @return A yamlconfiguration
     */
    public YamlConfiguration config() {
        return getFileFromCache("sierra.yml");
    }

    /**
     * The getFileFromCache function is used to retrieve a file from the cache.
     * If the file does not exist in the cache, it will be loaded and added to
     * the cache before being returned. This function should only be called if you
     * are sure that you want to load a new configuration into memory, as this can
     * cause performance issues if done too often. It is recommended that you use {
     *
     * @return A yamlconfiguration object
     */
    private YamlConfiguration getFileFromCache(String name) {
        YamlConfiguration configuration = cache.get(name);
        return configuration != null ? configuration : getFileFromName(name);
    }

    /**
     * The getFileFromName function is used to get a file from the plugin's resources folder.
     * It first checks if the file has already been loaded into memory, and if so, returns it.
     * If not, it loads the resource from disk and caches it in memory for future use.
     *
     * @param name name Get the file from the cache
     * @return A yamlconfiguration
     */
    private YamlConfiguration getFileFromName(String name) {

        Sierra.getPlugin().saveResource(name, false);
        File              file = new File("plugins/Sierra/" + name);
        YamlConfiguration yamlConfiguration;

        try {
            InputStream       inputStream = Files.newInputStream(file.toPath());
            InputStreamReader reader      = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            yamlConfiguration = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cache.put(name, yamlConfiguration);
        return yamlConfiguration;
    }

    /**
     * The invalidateCache function clears the cache of all entries.
     */
    public void invalidateCache() {
        cache.clear();
    }
}
