package org.mcwonderland.uhc.core.match;

public enum MatchState {
    WAITING,
    TELEPORTING,
    PRE_START,
    PLAYING;

    public boolean isLobby() {
        return this == WAITING;
    }

    public boolean isTeleport() {
        return this == TELEPORTING || this == PRE_START;
    }

    public boolean isStarted() {
        return this == PLAYING;
    }

    public MatchState nextState() {
        int nextIndex = ordinal() + 1;

        if (nextIndex >= values().length)
            throw new IllegalStateException("Match state " + this + " does not have a next state.");

        return values()[nextIndex];
    }
}
