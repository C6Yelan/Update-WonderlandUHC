package org.mcwonderland.uhc.legacy;

import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompSound;

public final class LegacyFoundationAdapter {

    private LegacyFoundationAdapter() {
    }

    public static void configureMenuClickSound() {
        Menu.setSound(new SimpleSound(CompSound.NOTE_STICKS.getSound(), 0, 0));
    }
}
