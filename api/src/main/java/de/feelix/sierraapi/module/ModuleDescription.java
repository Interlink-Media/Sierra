package de.feelix.sierraapi.module;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

/**
 * This class represents a module description, containing information about a module.
 * <p>
 * The module description includes the name, main class, author, type, and version of the module.
 * <p>
 * It also provides a method to read a URL string.
 */
@Getter
public class ModuleDescription {

    /**
     * ModuleDescription class represents a module description, containing information about a module.
     * The module description includes the name, main class, author, type, and version of the module.
     * It also provides a method to read a URL string.
     */
    private final String name;

    /**
     * Main class field of the ModuleDescription class.
     * <p>
     * This field stores the fully qualified name of the main class for the module.
     *
     * @see ModuleDescription
     */
    private final String main;

    /**
     * The author of the module description.
     * <p>
     * This field stores the name of the author of the module description.
     * The author is a string that represents the person who created the module.
     * <p>
     * Example usage:
     *    ModuleDescription module = new ModuleDescription(properties);
     *    String author = module.getAuthor();
     * <p>
     *    // Output: "John Doe"
     *    System.out.println(author);
     *
     * @see ModuleDescription
     */
    private final String author;

    /**
     * The type of the module.
     * <p>
     * This field holds the type of the module, as specified in the properties file.
     * The type is a string that represents the category or classification of the module.
     * It can be used to differentiate between different types of modules.
     * <p>
     * Example usage:
     *    ModuleDescription module = new ModuleDescription(properties);
     *    String moduleType = module.getType();
     * <p>
     *    // Output: "Plugin"
     *    System.out.println(moduleType);
     */
    private final String type;

    /**
     * The version of the module.
     */
    private String version;

    /**
     * Constructs a ModuleDescription object using the provided properties.
     *
     * @param properties the properties object containing the module description information
     */
    public ModuleDescription(Properties properties) {
        this.name = properties.getProperty("name");
        this.main = properties.getProperty("main");
        this.version = properties.getProperty("version");
        this.author = properties.getProperty("author");
        this.type = properties.getProperty("type");

        if (this.version.startsWith("http://") || this.version.startsWith("https://")) {
            this.version = this.readURLString(this.version);
        }
    }

    /**
     * Reads the content of a web page specified by the given URL string.
     *
     * @param urlString the URL string of the web page to read
     * @return the content of the web page as a string
     */
    public String readURLString(String urlString) {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder buffer = new StringBuilder();

            char[] chars = new char[1024];
            int    read;
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
