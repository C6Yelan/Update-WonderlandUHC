package org.mcwonderland.uhc.application.match;

import org.bukkit.command.CommandSender;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameTimerRunnable;
import org.mcwonderland.uhc.game.StateName;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;

public final class MatchStartRequestService {

    public void requestStart(CommandSender sender) {
        if (Game.getGame().getCurrentStateName() != StateName.WAITING) {
            Chat.send(sender, Messages.Host.GAME_RUNNING);
            return;
        }

        GameTimerRunnable.RUN = true;
        Extra.sound(Sounds.Countdown.Lobby.START);
    }
}
