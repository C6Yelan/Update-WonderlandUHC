package org.mcwonderland.uhc.application.match;

public final class CombatRelogTickResult {
    private final CombatRelogTickAction action;
    private final int nextRemainingSeconds;

    private CombatRelogTickResult(CombatRelogTickAction action, int nextRemainingSeconds) {
        if (action == null)
            throw new IllegalArgumentException("action cannot be null.");

        this.action = action;
        this.nextRemainingSeconds = nextRemainingSeconds;
    }

    public static CombatRelogTickResult of(CombatRelogTickAction action, int nextRemainingSeconds) {
        return new CombatRelogTickResult(action, nextRemainingSeconds);
    }

    public CombatRelogTickAction getAction() {
        return action;
    }

    public int getNextRemainingSeconds() {
        return nextRemainingSeconds;
    }
}
