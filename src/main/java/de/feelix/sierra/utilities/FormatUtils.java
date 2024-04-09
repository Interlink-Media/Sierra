package de.feelix.sierra.utilities;

import org.bukkit.ChatColor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class FormatUtils {

    /**
     * The toInt function takes a String input and converts it to an integer.
     * If the conversion fails, the function returns 1.
     *
     * @param input input Convert the string to an int
     * @return The integer value of the input string
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

    public static String formatBoolean(boolean... booleans) {
        StringBuilder builder = new StringBuilder();
        for (boolean aBoolean : booleans) {
            builder.append(aBoolean ? "+" : "-");
        }
        return builder.toString();
    }

    public static String shortenString(String input) {
        if (input.length() > 50) {
            return input.substring(0, 50);
        } else {
            return input;
        }
    }

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
     * The convertMillisToTicks function converts a given number of milliseconds to the equivalent number of ticks.
     *
     * @param millis millis Convert the time in milliseconds to ticks
     * @return The number of ticks that correspond to the given amount of milliseconds
     */
    public static int convertMillisToTicks(long millis) {
        return (int) (millis * 20 / 1000);
    }

    /**
     * The formatTimestamp function takes a long timestamp and returns a string
     * formatted as &quot;MM/dd HH:mm:ss&quot; where MM is the month, dd is the day of the
     * month, HH is hours in 24-hour format (00-23), mm are minutes, and ss are seconds.
     *
     * @param timestamp timestamp Convert the timestamp into a date
     * @return A string
     */
    public static String formatTimestamp(long timestamp) {
        Date             date = new Date(timestamp);
        SimpleDateFormat sdf  = new SimpleDateFormat("MM/dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * The formatColor function takes a string and replaces all instances of the '&amp;' character with the ChatColor
     * .COLOR_CHAR
     * character, allowing for easy use of Bukkit's built-in color codes.
     *
     * @param text text Get the text that is going to be formatted
     * @return A string with color codes replaced by their corresponding color
     */
    public static String formatColor(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
