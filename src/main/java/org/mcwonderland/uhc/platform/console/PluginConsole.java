package org.mcwonderland.uhc.platform.console;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;
import java.util.logging.Level;

public final class PluginConsole {
    private static final String LOG_PREFIX = "[WonderlandUHC]";
    private static final String CONSOLE_LINE = "!-----------------------------------------------------!";
    private static final String CONSOLE_LINE_SMOOTH = "______________________________________________________________";
    private static final char LEGACY_COLOR_CHAR = '\u00A7';
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)" + LEGACY_COLOR_CHAR + "[0-9A-FK-ORX]");

    private PluginConsole() {
    }

    public static void log(String... messages) {
        logWithPrefix(true, messages);
    }

    public static void logNoPrefix(String... messages) {
        logWithPrefix(false, messages);
    }

    public static void logFramed(String... messages) {
        if (messages == null || messages.length == 0)
            return;

        log("&7" + CONSOLE_LINE);

        for (String message : messages)
            log(" &c" + message);

        log("&7" + CONSOLE_LINE);
    }

    public static void error(Throwable throwable, String... messages) {
        Throwable unwrapped = unwrap(throwable);
        Throwable rootCause = rootCause(unwrapped);
        String error = errorSummary(rootCause);

        Bukkit.getLogger().log(Level.SEVERE, LOG_PREFIX + " " + error, unwrapped);

        if (messages == null || messages.length == 0)
            logFramed("&c" + error);
        else
            logFramed(replaceError(messages, error));
    }

    public static String consoleLineSmooth() {
        return CONSOLE_LINE_SMOOTH;
    }

    private static void logWithPrefix(boolean addPrefix, String... messages) {
        if (messages == null)
            return;

        CommandSender console = Bukkit.getConsoleSender();

        for (String message : messages) {
            if (message == null || "none".equals(message))
                continue;

            if (stripColors(message).replace(" ", "").isEmpty()) {
                console.sendMessage("  ");
                continue;
            }

            String colorized = colorize(message);

            for (String part : colorized.split("\n")) {
                String line = (prefix(addPrefix) + part).trim();
                console.sendMessage(line);
            }
        }
    }

    private static String prefix(boolean addPrefix) {
        return addPrefix ? LOG_PREFIX + " " : "";
    }

    private static String colorize(String message) {
        char[] chars = message.toCharArray();

        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = LEGACY_COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }

        return new String(chars);
    }

    private static String stripColors(String message) {
        return LEGACY_COLOR_PATTERN.matcher(colorize(message)).replaceAll("");
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof InvocationTargetException && throwable.getCause() != null)
            return throwable.getCause();

        return throwable == null ? new RuntimeException("Unknown error.") : throwable;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;

        while (cause.getCause() != null)
            cause = cause.getCause();

        return cause;
    }

    private static String errorSummary(Throwable throwable) {
        String name = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();

        if (message == null || message.isEmpty())
            return name;

        return name + ": " + message;
    }

    private static String[] replaceError(String[] messages, String error) {
        String[] replaced = new String[messages.length];

        for (int i = 0; i < messages.length; i++) {
            String message = messages[i];
            replaced[i] = message == null ? null : message
                    .replace("%error%", error)
                    .replace("%error", error);
        }

        return replaced;
    }
}
