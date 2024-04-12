package de.feelix.sierra.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NBTDetector {

    /**
     * The stringList variable is a private static final List of type String.
     * It is used in the NBTDetector class to store a list of specific strings
     * that are commonly found in NBT tags.
     * <p>
     * Example usage:
     * NBTDetector detector = new NBTDetector();
     * boolean result = detector.find("sample {nbt} input");
     * <p>
     * public class NBTDetector {
     * <p>
     *     private static final List<String> stringList = new ArrayList<>();
     * <p>
     *     public NBTDetector() {
     *         stringList.add("{");
     *         stringList.add("}");
     *         stringList.add("nbt");
     *     }
     * <p>
     *     public boolean find(String input) {
     *         // implementation
     *     }
     * }
     */
    private static final List<String> stringList = new ArrayList<>();

    /**
     * The NBTDetector class is responsible for detecting the presence of NBT tags in a given string.
     */
    public NBTDetector() {
        // stringList.add("["); falses
        // stringList.add("]"); falses
        stringList.add("{");
        stringList.add("}");
        // stringList.add("@"); // Bug when type @a or @e
        // stringList.add("=");
        stringList.add("nbt");
    }

    /**
     * Returns true if the given string contains an NBT tag, otherwise returns false.
     *
     * @param input the string to be checked for the presence of NBT tags
     * @return true if the string contains an NBT tag, false otherwise
     */
    public boolean find(String input) {
        // Regular expression to match NBT tags
        String  pattern = "\\{[^{}]*}";
        Pattern p       = Pattern.compile(pattern);
        Matcher m       = p.matcher(input);
        // If a match is found, return true

        if (m.find()) return true;

        for (String string : stringList) {
            if (input.contains(string)) {
                return true;
            }
        }
        return false;
    }
}
