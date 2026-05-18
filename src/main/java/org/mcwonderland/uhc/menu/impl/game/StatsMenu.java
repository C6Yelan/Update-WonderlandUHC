package org.mcwonderland.uhc.menu.impl.game;

import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.stats.UHCStats;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 2019-12-02 下午 11:12
 */
public class StatsMenu extends PluginMenu {

    private final Map<Integer, ItemStack> items = new HashMap<>();

    public StatsMenu(Player player) {
        super(PluginMenuSection.of("Stats"));

        UHCPlayer uhcPlayer = UHCPlayer.getUHCPlayer(player);
        UHCStats stats = uhcPlayer.getStats();

        int played = stats.gamePlayed;
        int wins = stats.totalWins;
        int kills = stats.totalKills;
        double kdr = stats.getKdr();

        addStatItem("Played", "{played}", played);
        addStatItem("Wins", "{wins}", wins);
        addStatItem("Kills", "{kills}", kills);
        addStatItem("Kdr", "{kdr}", kdr);
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        ItemStack item = items.get(slot);
        return item == null ? null : new ItemStack(item);
    }

    private void addStatItem(String buttonName, String placeholder, Object value) {
        items.put(getSection().getButtonSlot(buttonName), getSection().getButtonItem(buttonName, placeholder, value));
    }
}
