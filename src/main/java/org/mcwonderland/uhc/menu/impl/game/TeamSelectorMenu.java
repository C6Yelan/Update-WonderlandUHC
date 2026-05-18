package org.mcwonderland.uhc.menu.impl.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;

import java.util.stream.Collectors;

public class TeamSelectorMenu extends PluginPagedMenu<UHCTeam> {

    private static final String SECTION = "Team_Selector";
    private static final String AVAILABLE_BUTTON = "Available";
    private static final String FULL_BUTTON = "Full";
    private static final String CREATE_OWN_TEAM_BUTTON = "Create_Your_Own";

    public static void updateMenu() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof TeamSelectorMenu)
                new TeamSelectorMenu().displayTo(player);
        }
    }

    public TeamSelectorMenu() {
        super(PluginMenuSection.of(SECTION), getOpenJoinTeams());
    }

    private static Iterable<UHCTeam> getOpenJoinTeams() {
        return UHCTeam.getTeams().stream().filter(UHCTeam::isOpenJoin).collect(Collectors.toList());
    }

    @Override
    protected ItemStack convertToItemStack(UHCTeam team) {
        return getSection().getButtonItem(
                getButtonName(team),
                "{slots}", team.getPlayersAmount(),
                "{max}", Game.getSettings().getTeamSettings().getTeamSize(),
                "{name}", team.getName(),
                "{color}", team.getColor(),
                "{character}", team.getSymbol(),
                "{players}", UHCPlayers.toNames(team.getPlayers())
        );
    }

    private String getButtonName(UHCTeam team) {
        return team.isFull() ? FULL_BUTTON : AVAILABLE_BUTTON;
    }

    @Override
    protected void onPageClick(Player player, UHCTeam clickedTeam, ClickType click) {
        UHCPlayer uhcPlayer = UHCPlayer.getUHCPlayer(player);
        UHCTeam previousTeam = uhcPlayer.getTeam();

        if (clickedTeam == previousTeam || clickedTeam.isFull())
            return;

        clickedTeam.join(uhcPlayer);
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        if (slot == getCreateOwnTeamButtonSlot())
            return getSection().getButtonItem(CREATE_OWN_TEAM_BUTTON);

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (slot == getCreateOwnTeamButtonSlot()) {
            player.closeInventory();
            player.performCommand("team create");
            return;
        }

        super.onClick(player, slot, click, clicked);
    }

    private int getCreateOwnTeamButtonSlot() {
        return getSection().getSize() - 5;
    }
}
