package org.mcwonderland.uhc.game.state.playing.listener;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.platform.event.PluginEvents;
import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.random.PluginRandom;
import org.mcwonderland.uhc.util.GameUtils;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.mcwonderland.uhc.util.WorldUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlockListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();

        if (!GameUtils.isGamingPlayer(player))
            e.setCancelled(true);
        else
            handleCustomEvents(e);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        UHCPlayer uhcPlayer = UHCPlayer.getUHCPlayer(e.getPlayer());

        if (uhcPlayer.isDead())
            e.setCancelled(true);
        return;
    }

    @EventHandler
    public void onBreakBlock(UHCBlockBreakEvent e) {
        if (PluginMaterials.isLeaves(e.getBlockType()))
            replaceAppleDrops(e.getDrops());
    }

    @EventHandler
    public void leaveDecay(LeavesDecayEvent e) {
        Block block = e.getBlock();
        List<ItemStack> drops = new ArrayList<>(block.getDrops());

        appleDrops(block, drops);
    }

    private void appleDrops(Block block, List<ItemStack> drops) {
        replaceAppleDrops(drops);
        block.setType(PluginMaterials.materialOf("AIR"));
        WorldUtils.dropItems(block.getLocation(), drops);
    }

    private void replaceAppleDrops(List<ItemStack> drops) {
        WorldUtils.replaceDrop(drops, PluginMaterials.materialOf("APPLE"), PluginMaterials.materialOf("AIR"));

        if (PluginRandom.chance(Game.getSettings().getAppleRate()))
            drops.add(PluginMaterials.itemOf("APPLE"));
    }

    private void handleCustomEvents(BlockBreakEvent e) {
        UHCBlockBreakEvent event = new UHCBlockBreakEvent(e);
        PluginEvents.callEvent(event);

        if (event.isDropsModified())
            handleCustomBlockDrops(event);
    }

    private void handleCustomBlockDrops(UHCBlockBreakEvent e) {
        Block block = e.getBlock();
        Location blockLocation = block.getLocation();

        dropItems(e, blockLocation);
        PlayerUtils.costPlayerToolDurability(e.getPlayer());

        block.setType(PluginMaterials.materialOf("AIR"));
    }

    private void dropItems(UHCBlockBreakEvent e, Location location) {
        e.getDrops().forEach(dropItem -> WorldUtils.dropItems(location, dropItem));
        WorldUtils.spawnOrb(location, e.getExpToDrop());
    }

}
