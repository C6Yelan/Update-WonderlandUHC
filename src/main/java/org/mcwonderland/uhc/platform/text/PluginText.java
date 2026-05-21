package org.mcwonderland.uhc.platform.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.mcwonderland.uhc.util.TimePlaceholderFormatter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public final class PluginText {
    private static final String DELIMITER = "\n";
    private static final String MINI_MESSAGE_FORMAT_TAGS = "black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|obfuscated|bold|strikethrough|underlined|italic|reset|#[0-9a-f]{6}";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final Pattern MINI_MESSAGE_FORMAT_PATTERN = Pattern.compile("(?i)</?(" + MINI_MESSAGE_FORMAT_TAGS + ")>");
    private static final DecimalFormat FIVE_DIGITS = new DecimalFormat("#.#####");

    private PluginText() {
    }

    public static String stripColors(String message) {
        if (message == null)
            return null;

        return PLAIN_TEXT.serialize(toComponent(message));
    }

    public static Component toComponent(String message) {
        if (hasMiniMessageFormatTag(message))
            return MINI_MESSAGE.deserialize(message);

        return Component.text(message);
    }

    public static Component toItemComponent(String message) {
        return toComponent(message).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static Component toNullableComponent(String message) {
        return message == null ? null : toComponent(message);
    }

    public static Component toMiniMessageComponent(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    public static String toMiniMessageString(Component component) {
        return MINI_MESSAGE.serialize(component);
    }

    public static Object formatted(String value) {
        return new FormattedText(value);
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
            Object value = replacements[i + 1];

            replaced = replacePlaceholder(replaced, placeholder, value);
        }

        return replaced;
    }

    private static boolean hasMiniMessageFormatTag(String message) {
        return MINI_MESSAGE_FORMAT_PATTERN.matcher(message).find();
    }

    private static String replacePlaceholder(String message, String placeholder, Object value) {
        String key = normalizePlaceholder(placeholder);
        String replacement = formatPlaceholderValue(message, value);
        boolean emptyColorless = plainPlaceholderValue(value).isEmpty();

        return message
                .replace("{" + key + "}", replacement)
                .replace("{" + key + "+}", emptyColorless ? "" : replacement + " ")
                .replace("{+" + key + "}", emptyColorless ? "" : " " + replacement)
                .replace("{+" + key + "+}", emptyColorless ? "" : " " + replacement + " ");
    }

    private static String formatPlaceholderValue(String message, Object value) {
        String text = placeholderValue(value);

        if (value instanceof FormattedText)
            return text;

        if (hasMiniMessageFormatTag(message))
            return MINI_MESSAGE.escapeTags(text);

        return text;
    }

    private static String placeholderValue(Object value) {
        if (value instanceof FormattedText formatted)
            return formatted.value();

        return String.valueOf(value);
    }

    private static String plainPlaceholderValue(Object value) {
        String text = placeholderValue(value);

        if (value instanceof FormattedText)
            return PLAIN_TEXT.serialize(toComponent(text));

        return text;
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

    private static final class FormattedText {

        private final String value;

        private FormattedText(String value) {
            this.value = value == null ? "" : value;
        }

        private String value() {
            return value;
        }
    }
}
