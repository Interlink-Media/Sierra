package de.feelix.sierra.utilities;

public class FieldReader {

    /**
     * The isNotReadable function checks if a string is readable.
     *
     * @param input input Check if the string is readable
     *
     * @return True if the input string contains only characters that are not readable
     */
    public static boolean isNotReadable(String input) {
        for (char c : input.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                return false;
            }
            if (c >= 'A' && c <= 'Z') {
                return false;
            }
            if (c == 'ö' || c == 'ß' || c == 'ä' || c == 'ü' || c == '̇') {
                return false;
            }
            if (c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8'
                || c == '9' || c == '0') {
                return false;
            }
            if (c == '!' || c == '\"' || c == '$' || c == '%' || c == '&' || c == '/' || c == '(' || c == ')'
                || c == '{' || c == '}' || c == '[' || c == ']' || c == '=' || c == '?' || c == '@' || c == '*'
                || c == '+' || c == '~' || c == '<' || c == '>' || c == '|' || c == ';' || c == ',' || c == ':'
                || c == '-' || c == '_' || c == '.') {
                return false;
            }
        }
        return true;
    }
}
