package org.mcwonderland.uhc.game.timer.impl.countdown;

import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.platform.event.PluginEvents;
import org.mcwonderland.uhc.api.enums.RoleName;
import org.mcwonderland.uhc.api.event.timer.UHCStartedEvent;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameManager;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.timer.Countdown;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.*;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class StartCountdown extends Countdown {
    private static final int VANILLA_FIRE_SPREAD_RADIUS = 128;

    @Override
    public void execute() {
        Game game = Game.getGame();
        UHCGameSettings settings = Game.getSettings();
        game.setAllPlayers(UHCPlayers.countOf(uhcPlayer -> uhcPlayer.getRoleName() == RoleName.PLAYER));
        game.nextState();
        setupWorlds();

        for (UHCPlayer uhcPlayer : UHCPlayer.getAllPlayers()) {
            Player player = uhcPlayer.getPlayer();

            if (uhcPlayer.isOnline()) {
                GameManager.unFreeze(player);
                if (GameUtils.isGamingPlayer(player)) {
                    player.setGameMode(GameMode.SURVIVAL);
                    Extra.comepleteClear(player);
                    InventorySaver.setContents(player, InventorySaver.SaveType.CUSTOM_INVENTORY);
                    if (settings.getInitialExperience() > 0)
                        player.setLevel(settings.getInitialExperience());
                }
            } else {
                if (uhcPlayer.getRoleName() == RoleName.PLAYER)
                    uhcPlayer.changeSpectatorRole();
            }
        }

        PluginEvents.callEvent(new UHCStartedEvent());
    }

    private static void setupWorlds() {
        for (World world : UHCWorldUtils.getUhcWorlds()) {
            world.setGameRule(GameRules.LOCATOR_BAR, false);
            world.setGameRule(GameRules.ADVANCE_TIME, true);
            world.setGameRule(GameRules.ADVANCE_WEATHER, true);
            world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, VANILLA_FIRE_SPREAD_RADIUS);
            world.setGameRule(GameRules.SPAWN_MOBS, true);
            world.setGameRule(GameRules.SPAWN_MONSTERS, true);
            world.setSpawnLocation(0, 0, 0);
            world.setDifficulty(Difficulty.HARD);
        }
    }

    @Override
    public int getToggleTimer() {
        return Settings.Game.TIME_TO_START_AFTER_TELEPORT;
    }

    @Override
    public String getCountdownBroadcast() {
        Extra.sound(Sounds.Countdown.Start.TICK);
        return Messages.CountDown.STARTING_ANNOUNCE;
    }

    @Override
    public String getToggledBroadcast() {
        Chat.broadcast(PluginText.replaceToArray(
                Messages.CountDown.GAME_STARTED,
                "{host}", Game.getGame().getHost()));
        Extra.sound(Sounds.Countdown.Start.RUN);
        return null;
    }
}
