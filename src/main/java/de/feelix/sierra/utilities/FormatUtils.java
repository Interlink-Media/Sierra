package de.feelix.sierra.utilities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Utility class for formatting strings, numbers, and collections.
 */
@SuppressWarnings("unused")
public class FormatUtils {

    /**
     * Converts a string representation of an integer to an integer value.
     *
     * @param input The string representation of the integer.
     * @return The integer value of the input string, or 1 if the input string is not a valid integer.
     */
    public static int toInt(String input) {
        int num;
        try {
            num = Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            num = 1;
        }
        return num;
    }

    /**
     * Shortens a given string if its length is greater than 50 characters.
     *
     * @param input The input string.
     * @return The shortened string if its length is greater than 50 characters, otherwise the input string as is.
     */
    public static String shortenString(String input) {
        if (input.length() > 50) {
            return input.substring(0, 50);
        } else {
            return input;
        }
    }

    // Taken from https://www.spigotmc.org/threads/mapping-protocol-to-bukkit-slots.577724/
    public static int getBukkitSlot(int packetSlot) {
        // 0 -> 5 are crafting slots, don't exist in bukkit
        if (packetSlot <= 4) {
            return -1;
        }
        // 5 -> 8 are armor slots in protocol, ordered helmets to boots
        if (packetSlot <= 8) {
            // 36 -> 39 are armor slots in bukkit, ordered boots to helmet. tbh I got this from trial and error.
            return (7 - packetSlot) + 36;
        }
        // By a coincidence, non-hotbar inventory slots match.
        if (packetSlot <= 35) {
            return packetSlot;
        }
        // 36 -> 44 are hotbar slots in protocol
        if (packetSlot <= 44) {
            // 0 -> 9 are hotbar slots in bukkit
            return packetSlot - 36;
        }
        // 45 is offhand is packet, it is 40 in bukkit
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)
            && packetSlot == 45) {
            return 40;
        }
        return -1;
    }

    /**
     * Converts a Map object to its string representation.
     *
     * @param map The Map object to be converted.
     * @param <K> The type of the keys in the Map.
     * @param <V> The type of the values in the Map.
     * @return The string representation of the Map.
     */
    public static <K, V> String mapToString(Map<K, V> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<K, V> entry : map.entrySet()) {
            sb.append(entry.getKey())
                .append("=")
                .append(entry.getValue())
                .append(", ");
        }
        if (!map.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts a List object to its string representation.
     *
     * @param <T>  the type of elements in the list
     * @param list the list to be converted to a string
     * @return the string representation of the list
     */
    public static <T> String listToString(List<T> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (T item : list) {
            sb.append(item).append(", ");
        }
        if (!list.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");
        return sb.toString();
    }


    /**
     * Converts a given time in milliseconds to ticks.
     *
     * @param millis The time in milliseconds to be converted.
     * @return The corresponding number of ticks.
     */
    public static int convertMillisToTicks(long millis) {
        return (int) (millis * 20 / 1000);
    }


    /**
     * Converts a given timestamp to a formatted string representation.
     *
     * @param timestamp The timestamp to be formatted.
     * @return The formatted string representation of the timestamp.
     */
    public static String formatTimestamp(long timestamp) {
        Date             date = new Date(timestamp);
        SimpleDateFormat sdf  = new SimpleDateFormat("MM/dd HH:mm:ss");
        return sdf.format(date);
    }


    /**
     * Translates a given text by replacing any color codes with their corresponding colors.
     *
     * @param text The text to be formatted.
     * @return The formatted text with color codes replaced.
     */
    public static BaseComponent[] formatColor(String text) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', text));
    }
}
