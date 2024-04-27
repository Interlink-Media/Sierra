package de.feelix.sierra.manager.modules.impl;

import de.feelix.sierraapi.module.ModuleDescription;
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
public class SierraModuleDescription implements ModuleDescription {

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
     * @see SierraModuleDescription
     */
    private final String main;

    /**
     * The author of the module description.
     * <p>
     * This field stores the name of the author of the module description.
     * The author is a string that represents the person who created the module.
     * <p>
     * Example usage:
     * ModuleDescription module = new ModuleDescription(properties);
     * String author = module.getAuthor();
     * <p>
     * // Output: "John Doe"
     * System.out.println(author);
     *
     * @see SierraModuleDescription
     */
    private final String author;

    /**
     * The version of the module.
     */
    private String version;

    /**
     * Constructs a ModuleDescription object using the provided properties.
     *
     * @param properties the properties object containing the module description information
     */
    public SierraModuleDescription(Properties properties) {
        this.name = properties.getProperty("name");
        this.main = properties.getProperty("main");
        this.version = properties.getProperty("version");
        this.author = properties.getProperty("author");

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

    /**
     * Returns the name of the module.
     *
     * @return the name of the module
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Returns the value of the main method of the module.
     *
     * @return the value of the main method
     */
    @Override
    public String main() {
        return this.main;
    }

    /**
     * Returns the author of the module.
     *
     * @return the author of the module
     */
    @Override
    public String author() {
        return this.author;
    }

    /**
     * Returns the version of the module.
     *
     * @return the version of the module
     */
    @Override
    public String version() {
        return this.version;
    }
}
