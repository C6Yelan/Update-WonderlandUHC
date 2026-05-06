package org.mcwonderland.uhc.legacy;

import org.bukkit.event.Event;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.SimpleReplacer;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompSound;

import java.util.List;
import java.util.function.Consumer;

public final class LegacyFoundationAdapter {

    @FunctionalInterface
    public interface CommandGroupRegistrar {

        void register(Object commandGroup);
    }

    private LegacyFoundationAdapter() {
    }

    public static void extractFile(String path) {
        FileUtil.extract(path);
    }

    public static void extractFile(String from, String to) {
        FileUtil.extract(from, to);
    }

    public static void log(String... messages) {
        Common.log(messages);
    }

    public static void logReplacing(List<String> messages, String placeholder, Object value) {
        Common.log(new SimpleReplacer(messages).replace(placeholder, value).toArray());
    }

    public static void logNoPrefix(String... messages) {
        Common.logNoPrefix(messages);
    }

    public static String consoleLineSmooth() {
        return Common.consoleLineSmooth();
    }

    public static void setTellPrefix(String prefix) {
        Common.setTellPrefix(prefix);
    }

    public static void callEvent(Event event) {
        Common.callEvent(event);
    }

    public static CommandGroupRegistrar commandGroupRegistrar(Consumer<SimpleCommandGroup> registerCommandGroup) {
        return commandGroup -> registerCommandGroup.accept((SimpleCommandGroup) commandGroup);
    }

    public static boolean isAtLeastMinecraft1_13() {
        return MinecraftVersion.atLeast(MinecraftVersion.V.v1_13);
    }

    public static void checkBoolean(boolean expression, String message) {
        Valid.checkBoolean(expression, message);
    }

    public static RuntimeException failure(String message) {
        return new FoException(message);
    }

    public static void configureMenuClickSound() {
        Menu.setSound(new SimpleSound(CompSound.NOTE_STICKS.getSound(), 0, 0));
    }
}
