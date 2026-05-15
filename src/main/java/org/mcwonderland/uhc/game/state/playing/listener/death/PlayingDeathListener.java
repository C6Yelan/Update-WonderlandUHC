package org.mcwonderland.uhc.game.state.playing.listener.death;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.events.UHCGamingDeathEvent;
import org.mcwonderland.uhc.game.CombatRelog;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameManager;
import org.mcwonderland.uhc.game.player.DeathPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.legacy.LegacyDatouNmsAdapter;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.mcwonderland.uhc.util.UHCWorldUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 2019-12-07 下午 07:02
 */
public class PlayingDeathListener implements Listener {
    private final Map<UHCGamingDeathEvent, List<ItemStack>> customDropsByEvent = new IdentityHashMap<>();

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        UHCPlayer uhcPlayer = UHCPlayer.getFromEntity(entity);

        if (uhcPlayer != null) {
            UHCGamingDeathEvent uhcDeathEvent = new UHCGamingDeathEvent(uhcPlayer, e);

            LegacyFoundationAdapter.callEvent(uhcDeathEvent);
            releaseCustomDropsIfStillPending(entity, e.getDrops(), customDropsByEvent.remove(uhcDeathEvent));
        }
    }

    @EventHandler
    public void removeDeathMessages(PlayerDeathEvent e) {
        if (Settings.Misc.USE_PLUGIN_DEATH_MESSAGE)
            e.deathMessage(null);
    }

    @EventHandler
    public void handleDeathMessageAndData(UHCGamingDeathEvent e) {
        new UHCDeathDataHandler(e).run();
    }

    @EventHandler
    public void onGamingEntityDeath(UHCGamingDeathEvent e) {
        UHCPlayer uhcPlayer = e.getUhcPlayer();
        Player player = uhcPlayer.getPlayer();

        uhcPlayer.markSpectatorRole();
        checkDeathKick(player);
        if (Settings.Misc.DEATH_ANIMATION)
            LegacyDatouNmsAdapter.current().playDeathAnimation(player);

        respawnPlayerAsSpectator(uhcPlayer);
    }

    private void respawnPlayerAsSpectator(UHCPlayer uhcPlayer) {
        LegacyFoundationAdapter.runLater(1, () -> {
            if (!uhcPlayer.isOnline() || !uhcPlayer.isDead())
                return;

            if (!PlayerUtils.respawnIfDead(uhcPlayer.getPlayer()))
                return;

            LegacyFoundationAdapter.runLater(1, () -> {
                if (uhcPlayer.isOnline() && uhcPlayer.isDead()) {
                    uhcPlayer.applyRoleStuff();
                    uhcPlayer.getPlayer().teleport(UHCWorldUtils.getMatchCenterLocation());
                }
            });
        });
    }

    private void checkDeathKick(Player player) {
        if (UHCPermission.BYPASS_KICK_DEATH.hasPerm(player))
            return;

        Integer seconds = Settings.Spectator.DEATH_KICK_SECONDS;
        Chat.send(player, Messages.Spectator.NO_PERM_TO_SPEC
                .replace("{time}", "" + seconds));

        LegacyFoundationAdapter.runLater(seconds * 20, () -> {
            LegacyFoundationAdapter.kickPlayer(
                    player,
                    LegacyFoundationAdapter.replaceToString(
                            Messages.Spectator.DEATH_KICK_MESSAGE,
                            "{player}", player.getName()));
        });
    }

    /*
    1. put deathplayer data // lowest
    2. swap inventory mode check // low
    3. put backpack and custom drops // normal
    4. other scenario modify drops or something.... //high
    5. timebomb // highest
    6. remove from player list and check win // monitor
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void handleDeathPlayerData(UHCGamingDeathEvent e) {
        setDropAsInventoryItem(e);
        DeathPlayer.addDeathPlayer(e.getUhcPlayer());
    }

    private void setDropAsInventoryItem(UHCGamingDeathEvent e) {
        List<ItemStack> drops = new ArrayList<>();

        UHCPlayer uhcPlayer = e.getUhcPlayer();
        CombatRelog relog = CombatRelog.get(uhcPlayer);

        if (relog == null)
            return;

        addDrops(drops, Arrays.asList(relog.getInventoryContent().getAllItems()));

        e.getDrops().clear();
        e.getDrops().addAll(drops);
    }

    private static void addDrops(List<ItemStack> drops, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (isDropItem(item))
                drops.add(item.clone());
        }
    }

    private static boolean isDropItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void handleDeathDrops(UHCGamingDeathEvent e) {
        List<ItemStack> customDrops = collectDropItems(Game.getSettings().getItemSettings().getCustomDrops());

        if (customDrops.isEmpty())
            return;

        e.getDrops().addAll(customDrops);
        customDropsByEvent.put(e, customDrops);
    }

    static List<ItemStack> collectDropItems(List<ItemStack> items) {
        List<ItemStack> drops = new ArrayList<>();

        if (items == null)
            return drops;

        addDrops(drops, items);
        return drops;
    }

    private void releaseCustomDropsIfStillPending(LivingEntity entity, List<ItemStack> finalDrops, List<ItemStack> customDrops) {
        if (entity == null || finalDrops == null || customDrops == null || customDrops.isEmpty())
            return;

        for (ItemStack customDrop : customDrops) {
            if (removeByIdentity(finalDrops, customDrop))
                entity.getWorld().dropItemNaturally(entity.getLocation(), customDrop);
        }
    }

    private boolean removeByIdentity(List<ItemStack> drops, ItemStack target) {
        Iterator<ItemStack> iterator = drops.iterator();

        while (iterator.hasNext()) {
            if (iterator.next() == target) {
                iterator.remove();
                return true;
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void removeGaming(UHCGamingDeathEvent e) {
        removeRelogCaches(e.getEntity());

        GameManager.checkWin();
    }

    private void removeRelogCaches(LivingEntity entity) {
        CombatRelog relog = CombatRelog.getByRelogEntity(entity);

        if (relog == null)
            return;

        relog.remove();
    }


}
