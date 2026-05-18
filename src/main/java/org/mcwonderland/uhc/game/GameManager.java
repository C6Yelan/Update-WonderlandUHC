package org.mcwonderland.uhc.game;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.platform.event.PluginEvents;
import org.mcwonderland.uhc.application.match.HandleDeathResult;
import org.mcwonderland.uhc.application.match.HandleDeathUseCase;
import org.mcwonderland.uhc.api.event.timer.GameEndEvent;
import org.mcwonderland.uhc.core.match.MatchState;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.model.freeze.FreezeMode;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private static final HandleDeathUseCase HANDLE_DEATH = new HandleDeathUseCase();

    public static void freeze(Player player) {
        FreezeMode freezeMode = Settings.Game.FREEZE_TYPE.getFreezeMode();
        freezeMode.freeze(player);
    }

    public static void unFreeze(Player player) {
        FreezeMode freezeMode = Settings.Game.FREEZE_TYPE.getFreezeMode();
        freezeMode.unFreeze(player);
    }

    public static Block getHighestBlock(World world, int x, int z) {
        for (int runY = world.getMaxHeight(); runY > 0; runY--) { // get highest block
            Block temp = world.getBlockAt(x, runY, z);
            if (!PluginMaterials.isAir(temp)
                    && !PluginMaterials.isLongGrass(temp.getType())
                    && !PluginMaterials.isDoublePlant(temp.getType())) {
                return temp;
            }
        }

        return world.getHighestBlockAt(x, z);
    }


    public static UHCTeam getWinner() {
        List<HandleDeathUseCase.TeamStatus<UHCTeam>> teams = new ArrayList<>();

        for (UHCTeam team : UHCTeam.getTeams()) {
            teams.add(HandleDeathUseCase.TeamStatus.of(team, team.isEliminate()));
        }

        HandleDeathResult<UHCTeam> result = HANDLE_DEATH.evaluate(teams);
        return result.getWinner();
    }

    public static void checkWin() {
        UHCTeam team = getWinner();

        if (team == null)
            return;

        if (Game.getGame().getActiveMatch().getState() == MatchState.ENDING)
            return;

        Game.getGame().endMatch();
        broadcastWinning(team);
        PluginEvents.callEvent(new GameEndEvent());
        CacheSaver.deleteCache();
    }

    private static void broadcastWinning(UHCTeam winner) {
        List<String> winningMsg = getWinningMsg(winner);

        Chat.broadcast(winningMsg.toArray(new String[0]));
        Extra.sound(Sounds.Game.WIN);
    }

    private static List<String> getWinningMsg(UHCTeam winner) {

        List<String> list = PluginText.replaceToList(
                Messages.Game.VICTORY_BROADCAST,
                "{winner}", winner.getName(),
                "{kills}", "" + winner.getKills(),
                "{host}", Game.getGame().getHost());

        List<String> winningMessage = new ArrayList<>();

        //todo 優化code
        for (String s : list) {
            if (s.contains("{players}")) {
                for (String name : UHCPlayers.toNames(winner.getPlayers())) {
                    winningMessage.add(s.replace("{players}", name));
                }
            } else {
                winningMessage.add(s);
            }
        }

        return winningMessage;
    }


    public static boolean isTeamFireDisabled(UHCPlayer p1, UHCPlayer p2) {
        if (p1.getTeam() == p2.getTeam() && !Game.getSettings().getTeamSettings().isAllowTeamFire())
            return true;

        return false;
    }
}
