package de.feelix.sierra.utilities;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import de.feelix.sierra.check.violation.Debug;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for formatting strings, numbers, and collections.
 */
@SuppressWarnings("unused")
@UtilityClass
public class FormatUtils {

    private static final int    INVALID_INT_DEFAULT  = 1;
    private static final double NANOSECONDS_PER_TICK = 1000000000.0 / 20.0;

    private static final String[] units = {
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
    };

    private static final String[] teens = {
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    };

    private static final String[] tens = {
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    };

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss");

    /**
     * Converts a given string to an integer.
     *
     * @param input The string to be converted to an integer.
     * @return The converted integer value, or INVALID_INT_DEFAULT if the input cannot be parsed.
     */
    public int toInt(String input) {
        int num;
        try {
            num = Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            num = INVALID_INT_DEFAULT;
        }
        return num;
    }

    /**
     * Chains together valid debug information from a list of Debug objects.
     *
     * @param debugList The list of Debug objects.
     * @return A string containing the valid debug information, separated by commas.
     */
    public static String chainDebugs(List<Debug<?>> debugList) {
        boolean isValid = debugList.stream().anyMatch(FormatUtils::isValidDebug);
        if (isValid) {
            return debugList.stream()
                .filter(FormatUtils::isValidDebug)
                .map(debug -> debug.getName() + ": " + debug.getInfo())
                .collect(Collectors.joining(", "));
        }
        return "";
    }

    /**
     * Calculates the sum of all the values in a given HashMap.
     *
     * @param map The HashMap containing the values to be summed.
     * @return The sum of all the values in the HashMap.
     */
    public static int sumValuesInHashMap(HashMap<PacketTypeCommon, Integer> map) {
        int sum = 0;
        for (Integer value : map.values()) {
            sum += value;
        }
        return sum;
    }

    /**
     * Checks if a Debug object is valid.
     *
     * @param debug The Debug object to be checked.
     * @return True if the Debug object is valid, False otherwise.
     */
    private static boolean isValidDebug(Debug<?> debug) {
        return !debug.getName().isEmpty() && !String.valueOf(debug.getInfo()).isEmpty();
    }

    /**
     * Calculates the result by dividing the given number by NANOSECONDS_PER_TICK.
     *
     * @param x The number to be divided.
     * @return The calculated result.
     */
    public static double calculateResult(double x) {
        return x / NANOSECONDS_PER_TICK;
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
     * Checks if a given input contains character spam.
     *
     * @param input     The input string to check for character spam.
     * @param threshold The maximum number of consecutive characters to consider as spam.
     * @return True if the input contains character spam, False otherwise.
     */
    public static boolean detectCharacterSpam(String input, int threshold) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        int count = 1;
        char previousChar = input.charAt(0);

        for (int i = 1; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if (currentChar == previousChar) {
                count++;
                if (count > threshold) {
                    return true;
                }
            } else {
                count = 1;
                previousChar = currentChar;
            }
        }
        return false;
    }

    /**
     * Checks the precision of a given double number.
     *
     * @param number The number to be checked.
     * @return True if the precision of the number is greater than 3, False otherwise.
     */
    public static boolean checkDoublePrecision(double number) {
        BigDecimal bd        = BigDecimal.valueOf(number);
        int        precision = bd.scale();
        return precision > 3;
    }

    /**
     * Converts a map to a string representation.
     *
     * @param map The map to be converted.
     * @return A string representation of the map, with key-value pairs separated by commas and enclosed in curly
     * brackets.
     */
    public static <K, V> String mapToString(Map<K, V> map) {
        StringJoiner sj = new StringJoiner(", ", "{", "}");
        for (Map.Entry<K, V> entry : map.entrySet()) {
            sj.add(entry.getKey() + "=" + entry.getValue());
        }
        return sj.toString();
    }

    /**
     * Counts the number of occurrences of a substring in a given input string.
     *
     * @param input     The input string to search for occurrences.
     * @param subString The substring to search for.
     * @return The number of occurrences of the substring in the input string.
     * @throws NullPointerException if input or subString is null.
     */
    public static int countOccurrences(String input, String subString) {
        Objects.requireNonNull(input, "Input string must not be null");
        Objects.requireNonNull(subString, "Substring must not be null");

        if (subString.isEmpty()) {
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
     * Converts a given number to its text representation.
     *
     * @param number The number to be converted.
     * @return The text representation of the given number.
     * Returns "Number out of range" if the number is less than 0 or greater than 100.
     */
    public static String numberToText(int number) {
        if (number < 0 || number > 100) {
            return "Number out of range";
        }
        if (number < 10) {
            return units[number];
        }
        if (number < 20) {
            return teens[number - 10];
        }
        if (number == 100) {
            return "one hundred";
        }
        int tenPlace  = number / 10;
        int unitPlace = number % 10;
        if (unitPlace == 0) {
            return tens[tenPlace];
        }
        return tens[tenPlace] + "-" + units[unitPlace];
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
     * Formats a timestamp to a formatted string representation.
     *
     * @param timestamp The timestamp to be formatted.
     * @return The formatted string representation of the timestamp.
     */
    public static String formatTimestamp(long timestamp) {
        // Convert Instant to LocalDateTime
        LocalDateTime dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        // Format the LocalDateTime
        return DATE_TIME_FORMATTER.format(dateTime);
    }
}
