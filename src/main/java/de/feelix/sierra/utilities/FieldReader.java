package de.feelix.sierra.utilities;

import lombok.experimental.UtilityClass;

/**
 * Utility class for determining the readability of a given input string.
 */
@UtilityClass
public class FieldReader {

    /**
     * Determines if the given input string is readable.
     *
     * @param input the input string to check for readability
     * @return true if the input string is readable, false otherwise
     */
    public boolean isReadable(String input) {
        for (char c : input.toCharArray()) {
            if (Character.isAlphabetic(c) || Character.isDigit(c) || !Character.isISOControl(c)) {
                continue;
            }
            return true;
        }
        return false;
    }
}
