package org.mcwonderland.uhc.game.border;

import lombok.Getter;
import org.mcwonderland.uhc.platform.text.PluginText;

/**
 * 2019-12-05 下午 11:05
 */
public enum BorderType {
    TIMER(new TimerBorder()),
    MOVING(new MovingBorder());

    @Getter
    private Border mode;

    BorderType(Border border) {
        this.mode = border;
    }

    public String fancyName() {
        return PluginText.bountifyCapitalized(name());
    }
}
