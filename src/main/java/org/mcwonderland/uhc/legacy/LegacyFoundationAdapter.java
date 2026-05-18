package org.mcwonderland.uhc.legacy;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompSound;

import java.util.function.Consumer;

public final class LegacyFoundationAdapter {

    @FunctionalInterface
    public interface CommandGroupRegistrar {

        void register(Object commandGroup);
    }

    private LegacyFoundationAdapter() {
    }

    public static void setTellPrefix(String prefix) {
        Common.setTellPrefix(prefix);
    }

    public static CommandGroupRegistrar commandGroupRegistrar(Consumer<SimpleCommandGroup> registerCommandGroup) {
        return commandGroup -> registerCommandGroup.accept((SimpleCommandGroup) commandGroup);
    }

    public static void configureMenuClickSound() {
        Menu.setSound(new SimpleSound(CompSound.NOTE_STICKS.getSound(), 0, 0));
    }
}
