package org.mcwonderland.uhc.core.match;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MatchSettings {
    private final String title;
    private final int maxPlayers;
    private final int teamSize;
    private final boolean allowTeamFire;
    private final boolean usingNether;
    private final String generator;
    private final Set<String> scenarios;

    public MatchSettings(String title, int maxPlayers, int teamSize, boolean allowTeamFire, boolean usingNether, String generator, Set<String> scenarios) {
        if (title == null || title.trim().isEmpty())
            throw new IllegalArgumentException("title cannot be blank.");

        if (maxPlayers < 1)
            throw new IllegalArgumentException("maxPlayers must be positive.");

        if (teamSize < 1)
            throw new IllegalArgumentException("teamSize must be positive.");

        if (generator == null)
            throw new IllegalArgumentException("generator cannot be null.");

        if (scenarios == null)
            throw new IllegalArgumentException("scenarios cannot be null.");

        this.title = title;
        this.maxPlayers = maxPlayers;
        this.teamSize = teamSize;
        this.allowTeamFire = allowTeamFire;
        this.usingNether = usingNether;
        this.generator = generator;
        this.scenarios = Collections.unmodifiableSet(new LinkedHashSet<>(scenarios));
    }

    public static MatchSettings defaults() {
        return new MatchSettings("&a&lWonderland&f&lUHC", 100, 1, false, false, "", Collections.<String>emptySet());
    }

    public String getTitle() {
        return title;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public boolean isAllowTeamFire() {
        return allowTeamFire;
    }

    public boolean isUsingNether() {
        return usingNether;
    }

    public String getGenerator() {
        return generator;
    }

    public Set<String> getScenarios() {
        return scenarios;
    }
}
