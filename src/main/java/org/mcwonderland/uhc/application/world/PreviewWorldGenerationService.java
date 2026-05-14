package org.mcwonderland.uhc.application.world;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.game.CenterCleaner;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;

import javax.annotation.Nullable;

public final class PreviewWorldGenerationService {

    public void create(Player player, boolean centerCleanerEnabled, @Nullable String seed) {
        if (player == null)
            throw new IllegalArgumentException("player cannot be null.");

        Game.getGame().setCenterCleaner(centerCleanerEnabled);
        Chat.send(player, CommandSettings.Uhc.Regen.CREATING_WORLD);
        Extra.sound(player, Sounds.Host.START_CREATING_WORLD);
        CenterCleaner.createWorld(Settings.Game.UHC_WORLD_NAME, player, centerCleanerEnabled, seed);
    }
}
