package org.mcwonderland.uhc.platform.text;

import org.mcwonderland.uhc.util.TimePlaceholderFormatter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public final class PluginText {
    private static final String DELIMITER = "\n";
    private static final char LEGACY_COLOR_CHAR = '\u00A7';
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)" + LEGACY_COLOR_CHAR + "[0-9A-FK-ORX]");
    private static final DecimalFormat FIVE_DIGITS = new DecimalFormat("#.#####");

    private PluginText() {
    }

    public static String colorize(String message) {
        if (message == null)
            return null;

        char[] chars = message.toCharArray();

        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = LEGACY_COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }

        return new String(chars);
    }

    public static String stripColors(String message) {
        if (message == null)
            return null;

        return LEGACY_COLOR_PATTERN.matcher(colorize(message)).replaceAll("");
    }

    public static String[] replaceToArray(String message, Object... replacements) {
        return replaceToString(message, replacements).split(DELIMITER);
    }

    public static String[] replaceToArray(List<String> messages, Object... replacements) {
        return replaceToArray(join(messages), replacements);
    }

    public static String replaceToString(String message, Object... replacements) {
        return replacePlaceholders(message, replacements);
    }

    public static String replaceToString(List<String> messages, Object... replacements) {
        return replaceToString(join(messages), replacements);
    }

    public static List<String> replaceToList(String message, Object... replacements) {
        return splitLines(replaceToString(message, replacements));
    }

    public static List<String> replaceToList(List<String> messages, Object... replacements) {
        return replaceToList(join(messages), replacements);
    }

    public static List<String> replaceJoinedToList(List<String> messages, String placeholder, Collection<?> values, String linePrefix) {
        String separator = DELIMITER + linePrefix;
        String replacement = separator + joinValues(values, separator);

        return replaceToList(join(messages), placeholder, replacement);
    }

    public static String[] replaceTimeToArray(List<String> messages, Number seconds) {
        return replaceToArray(join(messages), TimePlaceholderFormatter.replacementPairs(seconds));
    }

    public static String replaceTimeToString(String message, Number seconds) {
        return replaceToString(message, TimePlaceholderFormatter.replacementPairs(seconds));
    }

    public static List<String> replaceTimeToList(List<String> messages, Number seconds) {
        return replaceToList(join(messages), TimePlaceholderFormatter.replacementPairs(seconds));
    }

    public static String replaceTimePlaceholders(String message, int seconds) {
        return replaceTimeToString(message, seconds);
    }

    public static String formatTime(int seconds) {
        return TimePlaceholderFormatter.clockTime(seconds);
    }

    public static double formatFiveDigits(double value) {
        if (Double.isNaN(value))
            throw new IllegalArgumentException("Value must not be NaN");

        return Double.parseDouble(FIVE_DIGITS.format(value).replace(",", "."));
    }

    public static String bountifyCapitalized(String name) {
        if (name == null || name.isEmpty())
            return "";

        String[] words = name.toLowerCase().replace("_", " ").split(" ");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty())
                continue;

            if (builder.length() > 0)
                builder.append(' ');

            builder.append(Character.toUpperCase(word.charAt(0)));

            if (word.length() > 1)
                builder.append(word.substring(1));
        }

        return builder.toString();
    }

    public static String bountifyCapitalized(Enum<?> value) {
        return value == null ? "" : bountifyCapitalized(value.name());
    }

    private static String replacePlaceholders(String message, Object... replacements) {
        if (message == null)
            return null;

        if (replacements == null || replacements.length == 0)
            return message;

        String replaced = message;

        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = String.valueOf(replacements[i]);
            String value = String.valueOf(replacements[i + 1]);

            replaced = replacePlaceholder(replaced, placeholder, value);
        }

        return replaced;
    }

    private static String replacePlaceholder(String message, String placeholder, String value) {
        String key = normalizePlaceholder(placeholder);
        String colorized = colorize(value);
        boolean emptyColorless = stripColors(colorized).isEmpty();

        return message
                .replace("{" + key + "}", colorized)
                .replace("{" + key + "+}", emptyColorless ? "" : colorized + " ")
                .replace("{+" + key + "}", emptyColorless ? "" : " " + colorized)
                .replace("{+" + key + "+}", emptyColorless ? "" : " " + colorized + " ");
    }

    private static String normalizePlaceholder(String placeholder) {
        String key = placeholder;

        if (key.startsWith("{"))
            key = key.substring(1);
        if (key.endsWith("}"))
            key = key.substring(0, key.length() - 1);

        return key;
    }

    private static String join(List<String> messages) {
        return String.join(DELIMITER, messages);
    }

    private static List<String> splitLines(String message) {
        List<String> list = new ArrayList<>();

        for (String line : message.split(DELIMITER))
            list.add(line);

        return list;
    }

    private static String joinValues(Collection<?> values, String separator) {
        List<String> valueStrings = new ArrayList<>();

        for (Object value : values)
            valueStrings.add(String.valueOf(value));

        return String.join(separator, valueStrings);
    }
}
