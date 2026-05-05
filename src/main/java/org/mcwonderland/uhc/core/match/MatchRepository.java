package org.mcwonderland.uhc.core.match;

public final class MatchRepository {
    private UhcMatch activeMatch;

    public UhcMatch createDefaultMatch() {
        activeMatch = UhcMatch.create();
        return activeMatch;
    }

    public UhcMatch getActiveMatch() {
        return activeMatch;
    }

    public void setActiveMatch(UhcMatch match) {
        if (match == null)
            throw new IllegalArgumentException("match cannot be null.");

        this.activeMatch = match;
    }

    public void clearActiveMatch() {
        this.activeMatch = null;
    }
}
