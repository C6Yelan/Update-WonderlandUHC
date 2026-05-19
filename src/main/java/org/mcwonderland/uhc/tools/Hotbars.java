package org.mcwonderland.uhc.tools;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.tools.lobby.SettingsBook;
import org.mcwonderland.uhc.tools.lobby.TeamSettingsItem;
import org.mcwonderland.uhc.tools.lobby.TeamsItem;
import org.mcwonderland.uhc.tools.spectator.NetherPlayersItem;
import org.mcwonderland.uhc.tools.spectator.OverworldPlayersItem;
import org.mcwonderland.uhc.tools.spectator.RandomTeleportItem;
import org.mcwonderland.uhc.tools.spectator.TeleportZeroZeroItem;
import org.mcwonderland.uhc.tools.spectator.ToggleGameModeItem;
import org.mcwonderland.uhc.tools.staff.StaffOptionsItem;
import org.bukkit.entity.Player;

public class Hotbars {

    public static void giveLobbyItems(Player p) {
        if (UHCPermission.ITEM_SETTINGS.hasPerm(p))
            SettingsBook.getInstance().set(p);

        if (WorldLoadingCacheState.getLoadingStatus() == LoadingStatus.DONE) {
            UHCTeam team = UHCTeam.getTeam(p);
            if (team != null) {
                if (team.getOwner().getName().equals(p.getName()))
                    giveTeamSettingsItem(p);
            }
            TeamsItem.getInstance().set(p);
        }
    }

    public static void giveTeamSettingsItem(Player p) {
        TeamSettingsItem.getInstance().set(p);
    }

    public static void giveSpecItems(Player p) {
        OverworldPlayersItem.getInstance().set(p);
        NetherPlayersItem.getInstance().set(p);
        RandomTeleportItem.getInstance().set(p);
        ToggleGameModeItem.getInstance().set(p);
        TeleportZeroZeroItem.getInstance().set(p);
    }

    public static void giveStaffAddon(Player p) {
        StaffOptionsItem.getInstance().set(p);
    }
}
