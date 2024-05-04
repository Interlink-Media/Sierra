package de.feelix.sierra.utilities;

import java.util.regex.Pattern;

/**
 * The NBTDetector class is used to check if a given string contains an NBT (Named Binary Tag) tag.
 */
public class NBTDetector {

    /**
     * Returns true if the given string contains an NBT tag, otherwise returns false.
     *
     * @param input the string to be checked for the presence of NBT tags
     * @return true if the string contains an NBT tag, false otherwise
     */
    public boolean find(String input) {
        return Pattern.compile("\\{[^{}]*}").matcher(input).find();
    }
}
