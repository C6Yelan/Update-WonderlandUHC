package org.mcwonderland.uhc.game.timer.impl.countdown;

import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.timer.Countdown;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.*;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LobbyCountdown extends Countdown {

    @Override
    public void execute() {
        Game game = Game.getGame();
        UHCGameSettings settings = Game.getSettings();

        BorderUtil.setInitialBorders();
        game.setCurrentBorder(settings.getBorderSettings().getInitialBorder());
        for (World world : UHCWorldUtils.getUhcWorlds())
            world.setGameRule(GameRules.LOCATOR_BAR, false);

        TeamModifier.splitTeams();

        for (Player p : PluginPlayers.onlinePlayers()) {
            p.closeInventory();

            if (GameUtils.isStaff(p))
                p.teleport(UHCWorldUtils.getMatchCenterLocation());
            else
                Extra.comepleteClear(p);
        }

        game.nextState();
    }

    @Override
    public int getToggleTimer() {
        return Settings.Game.PRE_START_TIME;
    }

    @Override
    public String getCountdownBroadcast() {
        Extra.sound(Sounds.Countdown.Lobby.TICK);
        return Messages.CountDown.SCATTER_ANNOUNCE;
    }

    @Override
    public String getToggledBroadcast() {
        Extra.sound(Sounds.Countdown.Lobby.RUN);
        return Messages.CountDown.SCATTER_STARTED;
    }
}
