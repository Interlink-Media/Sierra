package de.feelix.sierra.utilities;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Utility class for formatting strings, numbers, and collections.
 */
@SuppressWarnings("unused")
@UtilityClass
public class FormatUtils {

    /**
     * Converts a string representation of an integer to an integer value.
     *
     * @param input The string representation of the integer.
     * @return The integer value of the input string, or 1 if the input string is not a valid integer.
     */
    public int toInt(String input) {
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
    public String shortenString(String input) {
        if (input.length() > 50) {
            return input.substring(0, 50);
        } else {
            return input;
        }
    }

    /**
     * Checks if the precision of a double number is greater than 3 decimal places.
     *
     * @param number The double number to check the precision of.
     * @return True if the precision of the number is greater than 3 decimal places, False otherwise.
     */
    public boolean checkDoublePrecision(double number) {
        String numberStr = Double.toString(number);
        if (!numberStr.contains(".")) {
            return false;
        }
        int decimalIndex = numberStr.indexOf(".");
        int precision    = numberStr.length() - decimalIndex - 1;
        return precision > 3;
    }

    /**
     * Converts a Map object to its string representation.
     *
     * @param map The Map object to be converted.
     * @param <K> The type of the keys in the Map.
     * @param <V> The type of the values in the Map.
     * @return The string representation of the Map.
     */
    public <K, V> String mapToString(Map<K, V> map) {
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
     * Counts the number of occurrences of a substring in a given input string.
     *
     * @param input     The input string to search for occurrences.
     * @param subString The substring to count occurrences of.
     * @return The number of occurrences of the substring in the input string.
     */
    public int countOccurrences(String input, String subString) {
        if (input == null || subString == null || subString.isEmpty()) {
            return 0;
        }

        int count = 0;
        int index = 0;

        while ((index = input.indexOf(subString, index)) != -1) {
            count++;
            index += subString.length();
        }

        return count;
    }

    /**
     * Converts a given time in milliseconds to ticks.
     *
     * @param millis The time in milliseconds to be converted.
     * @return The corresponding number of ticks.
     */
    public int convertMillisToTicks(long millis) {
        return (int) (millis * 20 / 1000);
    }

    /**
     * Converts a given timestamp to a formatted string representation.
     *
     * @param timestamp The timestamp to be formatted.
     * @return The formatted string representation of the timestamp.
     */
    public String formatTimestamp(long timestamp) {
        Date             date = new Date(timestamp);
        SimpleDateFormat sdf  = new SimpleDateFormat("MM/dd HH:mm:ss");
        return sdf.format(date);
    }
}
