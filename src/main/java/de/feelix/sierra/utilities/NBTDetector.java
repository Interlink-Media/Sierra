package de.feelix.sierra.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NBTDetector {

    private static final List<String> stringList = new ArrayList<>();

    public NBTDetector() {
        stringList.add("[");
        stringList.add("]");
        stringList.add("{");
        stringList.add("}");
        // stringList.add("@"); // Bug when type @a or @e
        // stringList.add("=");
        stringList.add("nbt");
    }

    /**
     * The containsNBTTag function checks if a string contains an NBT tag.
     *
     * @param input input Find the nbt tag in the string
     *              public string getnbttag(string input) {
     * @return A boolean value
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
