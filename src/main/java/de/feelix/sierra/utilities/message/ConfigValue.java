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
     * Represents a configurable message value.
     */
    private String message;

    /**
     * The ConfigValue class represents a configurable message in the plugin.
     * It is used to retrieve and manipulate message values from the plugin configuration.
     */
    public ConfigValue(String messageKey, String messageOnFailure, boolean messageFile) {
        if (messageFile) {
            this.message = Sierra.getPlugin()
                .getSierraConfigEngine()
                .messages()
                .getString(messageKey, messageOnFailure);
        } else {
            this.message = Sierra.getPlugin()
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
        this.message = ChatColor.translateAlternateColorCodes('&', this.message);
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
        this.message = this.message.replace(key, value);
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
