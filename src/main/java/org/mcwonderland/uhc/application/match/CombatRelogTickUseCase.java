package org.mcwonderland.uhc.application.match;

public final class CombatRelogTickUseCase {

    public CombatRelogTickResult tick(int remainingSeconds, boolean borderMoving, boolean insideBorder) {
        if (remainingSeconds <= 0)
            return CombatRelogTickResult.of(CombatRelogTickAction.EXPIRE, remainingSeconds - 1);

        if (borderMoving && !insideBorder)
            return CombatRelogTickResult.of(CombatRelogTickAction.DAMAGE_OUTSIDE_BORDER, remainingSeconds - 1);

        return CombatRelogTickResult.of(CombatRelogTickAction.WAIT, remainingSeconds - 1);
    }
}
