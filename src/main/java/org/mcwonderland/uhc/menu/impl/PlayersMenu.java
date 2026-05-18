package org.mcwonderland.uhc.menu.impl;

import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;
import org.mcwonderland.uhc.util.GameUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PlayersMenu extends PluginPagedMenu<Player> {

    public PlayersMenu(Iterable<Player> pages, PluginMenuSection section) {
        super(section, pages);
    }

    public static PlayersMenu gamingPlayersMenu(World world, PluginMenuSection section) {
        return new PlayersMenu(getWorldPlayers(world), section);
    }

    private static Collection<Player> getWorldPlayers(World world) {
        return UHCPlayers.onlineStream()
                .filter(uhcPlayer -> !uhcPlayer.isDead() && uhcPlayer.getPlayer().getWorld() == world)
                .sorted(Comparator.comparing(UHCPlayer::getName))
                .map(UHCPlayer::getPlayer)
                .collect(Collectors.toList());
    }

    @Override
    protected ItemStack convertToItemStack(Player target) {
        ItemStack item = PluginItems.create(Material.PLAYER_HEAD, target.getName(), List.of());

        if (Bukkit.getOnlineMode())
            setOwner(item, target);

        return item;
    }

    @Override
    protected void onPageClick(Player player, Player target, ClickType click) {
        GameUtils.spectateTeleport(player, target);
    }

    private void setOwner(ItemStack item, Player target) {
        ItemMeta meta = item.getItemMeta();

        if (!(meta instanceof SkullMeta skullMeta))
            return;

        skullMeta.setOwningPlayer(target);
        item.setItemMeta(skullMeta);
    }
}
