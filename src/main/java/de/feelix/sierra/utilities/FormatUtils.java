package de.feelix.sierra.utilities;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import de.feelix.sierra.check.violation.Debug;
import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    public static String chainDebugs(boolean commaSeparated, List<Debug<?>> debugList) {

        boolean isValid = debugList.stream()
            .anyMatch(debug -> !debug.getName().isEmpty() && !String.valueOf(debug.getInfo()).isEmpty());

        if (isValid) {
            if (commaSeparated) {
                return debugList.stream()
                    .filter(debug -> !debug.getName().isEmpty() && !String.valueOf(debug.getInfo()).isEmpty())
                    .map(debug -> debug.getName() + ": " + debug.getInfo())
                    .collect(Collectors.joining(", "));
            } else {
                return "\n\n&4Info:\n" +
                       debugList.stream()
                           .filter(debug -> !debug.getName().isEmpty() && !String.valueOf(debug.getInfo()).isEmpty())
                           .map(debug -> "&7" + debug.getName() + ": &f" + debug.getInfo() + "\n")
                           .collect(Collectors.joining());
            }
        }
        return "";
    }

    public static int sumValuesInHashMap(HashMap<PacketTypeCommon, Integer> map) {
        int sum = 0;
        for (Integer value : map.values()) {
            sum += value;
        }
        return sum;
    }

    public static double calculateResult(double x) {
        double nanosPerTick = 1000000000.0 / 20.0;
        return x / nanosPerTick;
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
     * Converts a given number to its textual representation.
     *
     * @param number The number to be converted.
     * @return The textual representation of the given number.
     */
    public static String numberToText(int number) {
        if (number < 0 || number > 9) {
            return "Number out of range";
        }

        return new String[]{"zero", "one", "two", "three", "four", "five", "six", "seven",
            "eight", "nine"}[number];
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
