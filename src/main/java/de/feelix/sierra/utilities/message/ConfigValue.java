package de.feelix.sierra.utilities.message;

import de.feelix.sierra.Sierra;
import lombok.Getter;
import org.bukkit.ChatColor;

/**
 * The ConfigValue class represents a configurable message in the plugin.
 * It is used to retrieve and manipulate message values from the plugin configuration.
 */
@Getter
public class ConfigValue {

    /**
     * The messageKey variable represents the key used to retrieve a message value from the plugin configuration.
     * It is a private final String, which means it cannot be modified once initialized and its value is accessible
     * only within the class.
     * The messageKey is used in the ConfigMessage class to fetch the corresponding message value from the plugin
     * configuration.
     * The value is set during the construction of a ConfigMessage object and is retrieved using the getter method.
     * The messageKey is an important attribute as it serves as a reference to identify and retrieve specific
     * messages from the configuration.
     */
    private final String messageKey;

    /**
     * Represents a configurable message value.
     */
    private String messageValue;

    /**
     * The ConfigValue class represents a configurable message in the plugin.
     * It is used to retrieve and manipulate message values from the plugin configuration.
     */
    public ConfigValue(String messageKey, String messageOnFailure, boolean messages) {
        this.messageKey = messageKey;
        if (messages) {
            this.messageValue = Sierra.getPlugin()
                .getSierraConfigEngine()
                .messages()
                .getString(messageKey, messageOnFailure);
        } else {
            this.messageValue = Sierra.getPlugin()
                .getSierraConfigEngine()
                .config()
                .getString(messageKey, messageOnFailure);
        }
    }

    /**
     * Colorizes the message value by replacing color codes with the corresponding color.
     * Color codes are represented by '&' followed by a color code or color name.
     * For example, '&a' represents the color green.
     *
     * @return The ConfigMessage object with the colorized message value.
     */
    public ConfigValue colorize() {
        this.messageValue = ChatColor.translateAlternateColorCodes('&', this.messageValue);
        return this;
    }

    /**
     * Replaces the specified prefix in the message value with the prefix defined in the Sierra class.
     * The prefix to be replaced is represented by "{prefix}".
     * This method returns the updated ConfigMessage object.
     *
     * @return The updated ConfigMessage object with the prefix replaced.
     */
    public ConfigValue replacePrefix() {
        this.replace("{prefix}", Sierra.PREFIX);
        return this;
    }

    /**
     * Replaces occurrences of a specified key in the message value with a specified value.
     *
     * @param key   The key to be replaced in the message value.
     * @param value The value to replace the key with in the message value.
     * @return The updated ConfigMessage object after the replacement.
     */
    public ConfigValue replace(String key, String value) {
        this.messageValue = this.messageValue.replace(key, value);
        return this;
    }

    /**
     * Replaces occurrences of "{n}" with a line break ("\n") in the message value.
     *
     * @return The updated ConfigMessage object after the replacement.
     */
    public ConfigValue stripped() {
        this.replace("{n}", "\n");
        return this;
    }
}
