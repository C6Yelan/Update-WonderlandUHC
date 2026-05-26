package org.mcwonderland.uhc.api.game;

import org.mcwonderland.uhc.game.settings.sub.UHCScoreboardSettings;
import org.mcwonderland.uhc.game.settings.sub.UHCItemSettings;

import java.util.Collection;

public interface UHCGameSettings {
    void setTitle(String title);

    void setMaxPlayers(int maxPlayers);

    void setAppleRate(int appleRate);

    void setInitialExperience(int initialExperience);

    void setUsingNether(boolean usingNether);

    void setEnderPearlDamage(boolean enderPearlDamage);

    void setWhitelistOn(boolean whitelistOn);

    String getTitle();

    int getMaxPlayers();

    int getAppleRate();

    int getInitialExperience();

    boolean isUsingNether();

    boolean isEnderPearlDamage();

    boolean isWhitelistOn();

    String getGenerator();

    UHCTimerSettings getTimer();

    UHCTeamSettings getTeamSettings();

    UHCBorderSettings getBorderSettings();

    UHCScoreboardSettings getScoreboardSettings();

    UHCItemSettings getItemSettings();

    Collection<String> getScenarios();
}
