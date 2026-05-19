package org.mcwonderland.uhc.platform.console;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.mcwonderland.uhc.platform.text.PluginText;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public final class PluginConsole {
    private static final String LOG_PREFIX = "[WonderlandUHC]";
    private static final String CONSOLE_LINE = "!-----------------------------------------------------!";
    private static final String CONSOLE_LINE_SMOOTH = "______________________________________________________________";

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

        log("<gray>" + CONSOLE_LINE + "</gray>");

        for (String message : messages)
            log(" <red>" + message + "</red>");

        log("<gray>" + CONSOLE_LINE + "</gray>");
    }

    public static void error(Throwable throwable, String... messages) {
        Throwable unwrapped = unwrap(throwable);
        Throwable rootCause = rootCause(unwrapped);
        String error = errorSummary(rootCause);

        Bukkit.getLogger().log(Level.SEVERE, LOG_PREFIX + " " + error, unwrapped);

        if (messages == null || messages.length == 0)
            logFramed("<red>" + error + "</red>");
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

            for (String part : message.split("\n")) {
                console.sendMessage(Component.text(prefix(addPrefix)).append(PluginText.toComponent(part)));
            }
        }
    }

    private static String prefix(boolean addPrefix) {
        return addPrefix ? LOG_PREFIX + " " : "";
    }

    private static String stripColors(String message) {
        return PluginText.stripColors(message);
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
