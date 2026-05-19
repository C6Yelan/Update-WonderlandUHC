package org.mcwonderland.uhc.core.match;

import org.mcwonderland.uhc.game.settings.UHCGameSettings;

public final class MatchSettingsMapper {

    private MatchSettingsMapper() {
    }

    public static MatchSettings fromGameSettings(UHCGameSettings settings) {
        if (settings == null)
            throw new IllegalArgumentException("settings cannot be null.");

        if (settings.getTeamSettings() == null)
            throw new IllegalArgumentException("teamSettings cannot be null.");

        return new MatchSettings(
                settings.getTitle(),
                settings.getMaxPlayers(),
                settings.getTeamSettings().getTeamSize(),
                settings.getTeamSettings().isAllowTeamFire(),
                settings.isUsingNether(),
                settings.getGenerator(),
                settings.getScenarios()
        );
    }
}
