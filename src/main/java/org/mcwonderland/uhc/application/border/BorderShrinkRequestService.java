package org.mcwonderland.uhc.application.border;

import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameTimerRunnable;
import org.mcwonderland.uhc.game.border.Border;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.settings.sub.UHCBorderSettings;

public final class BorderShrinkRequestService {

    public int maxRequestedSize() {
        return borderSettings().getInitialBorder();
    }

    public void requestShrink(int size) {
        UHCGameSettings settings = Game.getSettings();
        Border border = settings.getBorderSettings().getBorderType().getMode();

        border.onCommand(size);
        settings.getTimer().setBorderShrinkTime(GameTimerRunnable.totalSecond + 11);
    }

    private UHCBorderSettings borderSettings() {
        return Game.getSettings().getBorderSettings();
    }
}
